# Event Store Fase 2 Specification

## Problem Statement

A Fase 1 provou o dominio puro: commands geram events, erros sao valores e o estado da conta e reconstruido por
`fold(eventos)`. A Fase 2 precisa trocar a lista de eventos em memoria por um event store persistente, mantendo a mesma
regra central: o estado nao e salvo como fonte da verdade; apenas eventos sao armazenados e depois reprocessados.

O objetivo de aprendizado agora e entender persistencia de eventos, versao de aggregate, concorrencia otimista e
reidratacao do aggregate sem trazer Kafka, projections ou API HTTP ainda.

## Goals

- [x] Definir uma porta de event store para salvar e carregar eventos por aggregate.
- [x] Persistir eventos de conta em PostgreSQL com payload serializado.
- [x] Carregar eventos em ordem e reconstruir `ContaBancaria` a partir do historico persistido.
- [x] Aplicar concorrencia otimista usando `expectedVersion`.
- [x] Manter testes de dominio independentes de Spring e criar testes de infraestrutura separados.

## Out of Scope

| Feature                  | Reason                                                                               |
|--------------------------|--------------------------------------------------------------------------------------|
| Kafka                    | So entra na Fase 3, depois que persistencia e reidratacao estiverem claras.          |
| Projections/read models  | So entram depois que o event store estiver confiavel.                                |
| API HTTP                 | Commands ainda podem ser exercitados por testes e servicos de aplicacao.             |
| Snapshot                 | Otimizacao prematura para o tamanho atual do projeto.                                |
| Saga de transferencia    | Continua reservada para Fase 4.                                                      |
| Event upcasting complexo | Pode ser estudado depois; nesta fase basta registrar `eventType` e versao do schema. |

---

## Design Direction

### Event Store Port

A aplicacao deve depender de uma abstracao de event store, nao diretamente do banco.

```text
EventStore
  append(aggregateId, expectedVersion, eventos)
  load(aggregateId)
```

Essa porta permite:

- testar fluxo de aplicacao com event store em memoria;
- implementar PostgreSQL sem contaminar o dominio;
- manter o aggregate puro.

### Evento Persistido

Eventos de dominio continuam sendo `ContaEvento`. Ao persistir, cada evento deve ganhar metadados.

```kotlin
data class EventoPersistido(
    val eventId: UUID,
    val aggregateId: UUID,
    val aggregateType: String,
    val aggregateVersion: Long,
    val eventType: String,
    val schemaVersion: Int,
    val occurredAt: Instant,
    val payload: String
)
```

Campos como `correlationId` e `causationId` podem ser previstos, mas nao precisam ser obrigatorios na primeira
implementacao da Fase 2.

Decisao atual de implementacao: `EventStoreRepository.load` retorna `EventoPersistido`, nao `ContaEvento`. A conversao
para evento de dominio fica em um mapper separado para manter o repository focado em persistencia.

### Tabela Inicial

```text
event_store
-----------
event_id UUID PK
aggregate_id UUID NOT NULL
aggregate_type TEXT NOT NULL
aggregate_version BIGINT NOT NULL
event_type TEXT NOT NULL
schema_version INT NOT NULL
occurred_at TIMESTAMP NOT NULL
payload JSONB NOT NULL
```

Restricao esperada:

```text
unique(aggregate_id, aggregate_version)
```

Essa restricao e a base da concorrencia otimista.

---

## User Stories

### P1: Salvar eventos de um aggregate MVP

**User Story**: Como estudante de Event Sourcing, quero salvar novos eventos de uma conta para transformar a lista em
memoria em historico persistente.

**Why P1**: Sem append de eventos nao existe event store.

**Acceptance Criteria**:

1. WHEN uma lista nao vazia de `ContaEvento` for aceita pelo dominio THEN o sistema SHALL persistir cada evento no event
   store.
2. WHEN eventos forem persistidos THEN o sistema SHALL atribuir `eventId`, `aggregateId`, `aggregateVersion`,
   `eventType`, `schemaVersion`, `occurredAt` e `payload`.
3. WHEN multiplos eventos forem persistidos em uma chamada THEN o sistema SHALL gravar versoes sequenciais.
4. WHEN a lista de eventos for vazia THEN o sistema SHALL nao gravar nada.

**Independent Test**: Processar `AbrirConta`, salvar `ContaAberta`, carregar o stream e verificar que existe 1 evento
persistido na versao 1.

---

### P1: Carregar stream de eventos MVP

**User Story**: Como estudante de Event Sourcing, quero carregar os eventos de uma conta em ordem para reconstruir o
aggregate.

**Why P1**: Event sourcing depende de replay ordenado.

**Acceptance Criteria**:

1. WHEN eventos forem carregados por `aggregateId` THEN o sistema SHALL retornar somente eventos desse aggregate.
2. WHEN eventos forem carregados THEN o sistema SHALL ordenar por `aggregateVersion` ascendente.
3. WHEN nao houver eventos para o `aggregateId` THEN o sistema SHALL retornar lista vazia.
4. WHEN eventos persistidos forem enviados ao mapper THEN o sistema SHALL desserializar `payload` para o subtipo correto
   de `ContaEvento`.

**Independent Test**: Persistir `ContaAberta`, `DinheiroDepositado` e `DinheiroSacado`; carregar stream e reconstruir
saldo por `fold`.

---

### P1: Reidratar aggregate

**User Story**: Como estudante de DDD com Event Sourcing, quero recriar `ContaBancaria` a partir do stream persistido
para processar novos commands.

**Why P1**: O aggregate nao deve depender de estado salvo em tabela tradicional.

**Acceptance Criteria**:

1. WHEN o repository carregar uma conta THEN o sistema SHALL buscar eventos no event store.
2. WHEN eventos forem encontrados THEN o sistema SHALL criar `ContaBancaria(eventosHistoricos)`.
3. WHEN nenhum evento for encontrado THEN o sistema SHALL permitir processar `AbrirConta` como nova conta.
4. WHEN um novo command for aceito THEN o repository SHALL salvar apenas os novos eventos gerados pelo `CommandAceito`.

**Independent Test**: Salvar abertura e deposito, reidratar aggregate, processar saque e verificar evento
`DinheiroSacado`.

---

### P1: Concorrencia otimista com expectedVersion

**User Story**: Como estudante de sistemas consistentes, quero impedir escrita concorrente sobre uma versao antiga do
aggregate.

**Why P1**: Event stores precisam proteger a ordem do stream e evitar perda de atualizacao.

**Acceptance Criteria**:

1. WHEN `append` receber `expectedVersion` igual a versao atual do stream THEN o sistema SHALL gravar os eventos.
2. WHEN `append` receber `expectedVersion` diferente da versao atual do stream THEN o sistema SHALL rejeitar a escrita
   com erro de concorrencia.
3. WHEN dois processos tentarem gravar a mesma proxima versao THEN apenas um SHALL vencer.
4. WHEN houver conflito THEN o sistema SHALL nao gravar nenhum evento parcial daquela chamada.

**Independent Test**: Carregar stream na versao 1, gravar outro evento ate versao 2, tentar gravar com
`expectedVersion = 1` e verificar rejeicao.

---

### P2: Serializacao JSON de eventos

**User Story**: Como estudante de persistencia de eventos, quero serializar eventos como JSON para inspecionar e
reconstruir fatos do dominio.

**Why P2**: JSON e simples de debugar e prepara o caminho para publicacao futura no Kafka.

**Acceptance Criteria**:

1. WHEN `ContaAberta` for persistido THEN o `payload` SHALL conter `id` e `saldoInicial`.
2. WHEN `DinheiroDepositado` for persistido THEN o `payload` SHALL conter `valor` e `idConta`.
3. WHEN `DinheiroSacado` for persistido THEN o `payload` SHALL conter `valor` e `idConta`.
4. WHEN `TransferenciaRealizada` for persistido THEN o `payload` SHALL conter `valor`, `idContaOrigem` e
   `idContaDestino`.
5. WHEN dinheiro for serializado THEN o sistema SHALL preservar precisao de `BigDecimal`.

**Independent Test**: Serializar e desserializar todos os subtipos atuais de `ContaEvento` e comparar com os eventos
originais.

---

### P2: Event store em memoria para aprendizado

**User Story**: Como estudante, quero uma implementacao em memoria da porta `EventStore` para testar fluxo de repository
sem depender de PostgreSQL em todos os testes.

**Why P2**: Ajuda a separar regra de aplicacao de detalhe de infraestrutura.

**Acceptance Criteria**:

1. WHEN eventos forem salvos no event store em memoria THEN eles SHALL ser recuperaveis por `aggregateId`.
2. WHEN `expectedVersion` divergir da versao atual THEN o event store em memoria SHALL rejeitar a escrita.
3. WHEN testes de aplicacao usarem event store em memoria THEN eles SHALL nao subir contexto Spring.

**Independent Test**: Usar event store em memoria para abrir conta, depositar e sacar via repository.

---

### P3: Metadados de rastreabilidade

**User Story**: Como estudante de sistemas event-driven, quero preparar metadados de rastreabilidade para conectar
eventos no futuro.

**Why P3**: `correlationId` e `causationId` serao importantes no Kafka, mas nao precisam bloquear o MVP da Fase 2.

**Acceptance Criteria**:

1. WHEN metadados forem informados no append THEN o sistema SHALL persistir `correlationId` e `causationId`.
2. WHEN metadados nao forem informados THEN o sistema SHALL permitir persistencia sem eles.
3. WHEN eventos forem carregados THEN metadados persistidos SHALL estar disponiveis para publicacao futura.

---

## Edge Cases

- WHEN `payload` nao puder ser desserializado THEN o sistema SHALL falhar explicitamente e nao retornar evento
  corrompido como valido.
- WHEN `eventType` for desconhecido THEN o sistema SHALL rejeitar desserializacao com erro claro.
- WHEN `aggregateVersion` tiver lacuna no stream THEN o sistema SHALL tratar o stream como inconsistente.
- WHEN `append` falhar por concorrencia THEN o sistema SHALL preservar os eventos originais sem escrita parcial.
- WHEN `BigDecimal` for serializado THEN o sistema SHALL preservar escala/precisao suficiente para dinheiro.
- WHEN eventos forem carregados fora de ordem THEN o repository SHALL ordenar por versao antes de reidratar o aggregate.

---

## Current Implementation Status

Atualizado em 2026-06-14.

- `EventStoreRepository.append` grava eventos em PostgreSQL com metadados minimos e `payload` em `jsonb`.
- Stream inexistente e tratado como versao `0`; o primeiro evento salvo recebe `aggregateVersion = 1`.
- Escrita de multiplos eventos gera versoes sequenciais na mesma chamada.
- `append` esta anotado com `@Transactional`.
- `append` rejeita conflito de versao com `ConflitoVersaoException`.
- `load` retorna `List<EventoPersistido>`, filtra por `aggregateId`, ordena por `aggregate_version` e usa
  `payload::text as payload` para trazer JSONB como string.
- `EventoPersistido` e apenas estrutura de dados, sem conhecer Jackson nem subtipos de evento.
- `EventoMapper` existe como componente separado em infraestrutura e converte `EventoPersistido` em todos os subtipos
  atuais de `ContaEvento`.
- A porta `EventStore` existe e `EventStoreRepository` implementa essa abstracao.
- `ContaBancariaRepository` existe, compoe `EventStore + EventoMapper` para reidratar `ContaBancaria` e possui 2 testes
  unitarios passando com `InMemoryEventStore`.
- `ContaBancariaService` possui 2 testes passando que cobrem o fluxo completo via `InMemoryEventStore`.
- `InMemoryEventStore` esta implementado, implementa a porta `EventStore` e simula concorrencia otimista por versao.
- `EventStoreRepositoryTest` possui 6 testes de integracao passando para persistencia, versao incorreta, multiplos
  eventos, atomicidade de lote, stream vazio e filtro/ordem por aggregate.
- `EventoMapperTest` possui 6 testes unitarios passando para `ContaAberta`, `DinheiroDepositado`, `DinheiroSacado`,
  `TransferenciaRealizada`, `ContaBloqueada` e `ContaEncerrada`.
- `mvnw test` passou em 2026-06-14 com 54 testes.

## Next Challenges

- Concluir ES2-07: adicionar `correlationId` e `causationId` opcionais ao `EventoPersistido` e ao schema da tabela
  `event_store`.
- Concluir ES2-08: cobrir edge cases de payload invalido, `eventType` desconhecido e lacuna de versao no stream.

---

## Requirement Traceability

| Requirement ID | Story                                         | Phase   | Status   |
|----------------|-----------------------------------------------|---------|----------|
| ES2-01         | P1: Salvar eventos de um aggregate            | Execute | Verified |
| ES2-02         | P1: Carregar stream de eventos                | Execute | Verified |
| ES2-03         | P1: Reidratar aggregate                       | Execute | Verified |
| ES2-04         | P1: Concorrencia otimista com expectedVersion | Execute | Verified |
| ES2-05         | P2: Serializacao JSON de eventos              | Execute | Verified |
| ES2-06         | P2: Event store em memoria                    | Execute | Verified |
| ES2-07         | P3: Metadados de rastreabilidade              | Specify | Pending  |
| ES2-08         | Edge cases de persistencia                    | Execute | Partial  |

**Coverage:** 8 total, 6 verified, 1 partial, 1 pending.

---

## Success Criteria

- [x] Eventos aceitos pelo dominio sao persistidos e carregados em ordem como `EventoPersistido`.
- [x] `ContaBancaria` pode ser reidratada a partir do event store.
- [x] `expectedVersion` impede escrita concorrente inconsistente em fluxo baseline.
- [x] JSON preserva os dados necessarios para replay dos eventos atuais.
- [x] Testes de dominio continuam sem Spring.
- [x] Testes de infraestrutura validam PostgreSQL separadamente.