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

```kotlin
data class EventoPersistido(
    val eventId: UUID,
    val aggregateId: UUID,
    val aggregateType: String,
    val aggregateVersion: Long,
    val eventType: String,
    val schemaVersion: Int,
    val occurredAt: LocalDateTime?,
    val payload: String,
    val causationId: UUID,
    val correlationId: UUID
)
```

`causationId` e preenchido com o `commandId` gerado pelo proprio command. `correlationId` fica reservado para a Fase 3,
quando a borda HTTP ou Kafka puder fornecer o valor externo.

### Decisoes de design

- `EventStoreRepository.load()` valida integridade do stream antes de retornar, detectando lacunas de versao via metodo
  privado `validarIntegridade`.
- `EventoMapper` captura excecoes de serializacao do Jackson e relanca como `PayloadInvalidoException`, evitando que
  detalhes de infraestrutura vazem para camadas superiores.
- Excecoes especificas do event store (`ConflitoVersaoException`, `EventoInconsistenteException`,
  `PayloadInvalidoException`) ficam em `domain/exception`.

---

## User Stories

### P1: Salvar eventos de um aggregate MVP

**Acceptance Criteria**:

1. WHEN uma lista nao vazia de `ContaEvento` for aceita pelo dominio THEN o sistema SHALL persistir cada evento no event
   store.
2. WHEN eventos forem persistidos THEN o sistema SHALL atribuir `eventId`, `aggregateId`, `aggregateVersion`,
   `eventType`, `schemaVersion`, `occurredAt` e `payload`.
3. WHEN multiplos eventos forem persistidos em uma chamada THEN o sistema SHALL gravar versoes sequenciais.
4. WHEN a lista de eventos for vazia THEN o sistema SHALL nao gravar nada.

---

### P1: Carregar stream de eventos MVP

**Acceptance Criteria**:

1. WHEN eventos forem carregados por `aggregateId` THEN o sistema SHALL retornar somente eventos desse aggregate.
2. WHEN eventos forem carregados THEN o sistema SHALL ordenar por `aggregateVersion` ascendente.
3. WHEN nao houver eventos para o `aggregateId` THEN o sistema SHALL retornar lista vazia.
4. WHEN eventos persistidos forem enviados ao mapper THEN o sistema SHALL desserializar `payload` para o subtipo correto
   de `ContaEvento`.

---

### P1: Reidratar aggregate

**Acceptance Criteria**:

1. WHEN o repository carregar uma conta THEN o sistema SHALL buscar eventos no event store.
2. WHEN eventos forem encontrados THEN o sistema SHALL criar `ContaBancaria(eventosHistoricos)`.
3. WHEN nenhum evento for encontrado THEN o sistema SHALL permitir processar `AbrirConta` como nova conta.
4. WHEN um novo command for aceito THEN o repository SHALL salvar apenas os novos eventos gerados pelo `CommandAceito`.

---

### P1: Concorrencia otimista com expectedVersion

**Acceptance Criteria**:

1. WHEN `append` receber `expectedVersion` igual a versao atual do stream THEN o sistema SHALL gravar os eventos.
2. WHEN `append` receber `expectedVersion` diferente da versao atual do stream THEN o sistema SHALL rejeitar a escrita
   com erro de concorrencia.
3. WHEN dois processos tentarem gravar a mesma proxima versao THEN apenas um SHALL vencer.
4. WHEN houver conflito THEN o sistema SHALL nao gravar nenhum evento parcial daquela chamada.

---

### P2: Serializacao JSON de eventos

**Acceptance Criteria**:

1. WHEN `ContaAberta` for persistido THEN o `payload` SHALL conter `id` e `saldoInicial`.
2. WHEN `DinheiroDepositado` for persistido THEN o `payload` SHALL conter `valor` e `idConta`.
3. WHEN `DinheiroSacado` for persistido THEN o `payload` SHALL conter `valor` e `idConta`.
4. WHEN `TransferenciaRealizada` for persistido THEN o `payload` SHALL conter `valor`, `idContaOrigem` e
   `idContaDestino`.
5. WHEN dinheiro for serializado THEN o sistema SHALL preservar precisao de `BigDecimal`.

---

### P2: Event store em memoria para aprendizado

**Acceptance Criteria**:

1. WHEN eventos forem salvos no event store em memoria THEN eles SHALL ser recuperaveis por `aggregateId`.
2. WHEN `expectedVersion` divergir da versao atual THEN o event store em memoria SHALL rejeitar a escrita.
3. WHEN testes de aplicacao usarem event store em memoria THEN eles SHALL nao subir contexto Spring.

---

### P3: Metadados de rastreabilidade

**Acceptance Criteria**:

1. WHEN um command for processado THEN o sistema SHALL persistir `causationId` derivado do `commandId`.
2. WHEN eventos forem carregados THEN `causationId` SHALL estar disponivel para rastreabilidade futura.
3. `correlationId` sera introduzido na Fase 3 quando a borda HTTP ou Kafka puder fornecer o valor externo.

---

## Edge Cases

- WHEN `payload` nao puder ser desserializado THEN o mapper SHALL lancar `PayloadInvalidoException` sem vazar excecoes
  do Jackson.
- WHEN `eventType` for desconhecido THEN o mapper SHALL lancar `IllegalArgumentException` com mensagem clara.
- WHEN `aggregateVersion` tiver lacuna no stream THEN `load` SHALL lancar `EventoInconsistenteException` com contagem e
  versao maxima.
- WHEN `append` falhar por concorrencia THEN o sistema SHALL preservar os eventos originais sem escrita parcial.
- WHEN `BigDecimal` for serializado THEN o sistema SHALL preservar escala/precisao suficiente para dinheiro.
- WHEN eventos forem carregados fora de ordem THEN o repository SHALL ordenar por versao antes de reidratar o aggregate.

---

## Current Implementation Status

Atualizado em 2026-06-20. Fase 2 concluida.

- `EventStoreRepository.append` grava eventos em PostgreSQL com `causationId` e `correlationId`.
- `EventStoreRepository.load` valida integridade do stream via `validarIntegridade` e lanca
  `EventoInconsistenteException` em caso de lacuna.
- `EventoMapper` captura excecoes do Jackson e relanca como `PayloadInvalidoException`.
- Todos os subtipos de `ContaEvento` possuem serializacao e desserializacao testadas.
- `InMemoryEventStore` implementa a porta `EventStore` e simula concorrencia otimista.
- `ContaBancariaRepository` reidrata `ContaBancaria` e possui 2 testes com `InMemoryEventStore`.
- `ContaBancariaService` possui 2 testes cobrindo fluxo completo via `InMemoryEventStore`.
- Excecoes `ConflitoVersaoException`, `EventoInconsistenteException` e `PayloadInvalidoException` em `domain/exception`.
- Todos os testes da Fase 2 passando.

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
| ES2-07         | P3: Metadados de rastreabilidade              | Execute | Verified |
| ES2-08         | Edge cases de persistencia                    | Execute | Verified |

**Coverage:** 8 total, 8 verified.

---

## Success Criteria

- [x] Eventos aceitos pelo dominio sao persistidos e carregados em ordem como `EventoPersistido`.
- [x] `ContaBancaria` pode ser reidratada a partir do event store.
- [x] `expectedVersion` impede escrita concorrente inconsistente em fluxo baseline.
- [x] JSON preserva os dados necessarios para replay dos eventos atuais.
- [x] Testes de dominio continuam sem Spring.
- [x] Testes de infraestrutura validam PostgreSQL separadamente.
- [x] Edge cases de persistencia cobertos com excecoes especificas e testes explicitos.