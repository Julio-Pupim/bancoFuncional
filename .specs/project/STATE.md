# Project State

## Decisions

- O projeto sera usado principalmente para aprendizado de DDD, Event Sourcing, CQRS, Event Driven e FP.
- A Fase 1 deve ser intencionalmente sem Kafka, banco de dados e Spring.
- O estado da conta nao sera a fonte da verdade; ele sera reconstruido por `fold(eventos)`.
- Eventos sao fatos imutaveis e commands representam intencao do usuario.
- Falhas esperadas de negocio devem ser modeladas como valores de dominio, nao como exceptions usadas para fluxo normal.
- Conta bloqueada, para fins didaticos da Fase 1, nao aceita deposito, saque nem transferencia.
- Transferencia na Fase 1 afeta apenas a conta origem e registra o destino por identificador; saga/process manager fica
  para uma fase posterior.
- Na Fase 2, stream inexistente no event store deve ser tratado como versao `0`.
- `EventStoreRepository` deve lidar com registros persistidos (`EventoPersistido`), enquanto a conversao para
  `ContaEvento` fica em um mapper separado.
- O payload fica como `jsonb` no PostgreSQL, mas volta para Kotlin como `String` JSON via `payload::text`.
- `append` deve ser transacional: se uma chamada tenta gravar multiplos eventos e algum falha, nenhum evento daquela
  chamada deve permanecer gravado.

## Current Focus

- Fase 1 concluida: dominio puro de `ContaBancaria` com commands, events, erros como valores e replay/fold.
- Fase 2 em execucao: maioria das entregas concluidas; pendentes ES2-07 (metadados de rastreabilidade) e ES2-08 (edge
  cases de persistencia).
- `EventStoreRepository.append` e `load` existem sobre PostgreSQL/Testcontainers.
- `append` grava eventos com versoes sequenciais, trata stream vazio como versao `0`, usa `@Transactional` e valida
  versao esperada contra `max(aggregate_version)`.
- `load` retorna `List<EventoPersistido>`, filtra por `aggregateId`, ordena por `aggregate_version` e le `payload` como
  JSON textual.
- `EventoMapper` converte todos os subtipos atuais de `ContaEvento` e possui 6 testes unitarios passando.
- A porta `EventStore` existe e `EventStoreRepository` implementa essa abstracao.
- `ContaBancariaRepository` existe, reidrata `ContaBancaria` a partir do event store e possui 2 testes unitarios
  passando usando `InMemoryEventStore`.
- `ContaBancariaService` possui 2 testes passando cobrindo o fluxo completo via `InMemoryEventStore`: abrir conta,
  depositar, sacar e encerrar.
- `InMemoryEventStore` esta implementado, implementa a porta `EventStore`, simula concorrencia otimista por versao e
  apoia testes de aplicacao sem Spring.
- `SaldoProjection` esta implementada com 4 testes unitarios passando usando `InMemorySaldoRepository`.
- `ExtratoProjection` esta implementada com 1 teste unitario passando usando `InMemoryExtratoRepository`.
- `mvnw test` passou com 54 testes: 33 de dominio, 2 de service, 2 de repositorio, 6 de mapper, 6 de event store
  PostgreSQL, 4 de SaldoProjection e 1 de ExtratoProjection.

## Deferred

- Kafka, consumers e wiring de projections ficam para a Fase 3.
- Transferencia distribuida entre duas contas fica para a Fase 4.
- API HTTP fica fora do primeiro escopo.
- `correlationId` e `causationId` (ES2-07) ainda pendentes; serao importantes para rastreabilidade na Fase 3.

## Concerns

- O modelo de dinheiro ja usa `BigDecimal`; manter cuidado nos testes para evitar `BigDecimal(100.00)` e preferir
  strings como `BigDecimal("100.00")`.
- O `pom.xml` ja inclui Spring, JDBC e Kafka, mas isso nao deve dirigir a Fase 1.
- `correlationId` e `causationId` continuam adiados; serao importantes ao publicar eventos no Kafka na Fase 3.
- O reducer/fold de eventos financeiros deve transformar o estado anterior. `DinheiroDepositado(10)` significa somar 10
  ao saldo atual, nao definir saldo como 10.
- `EventStoreRepository.append` ja lanca `ConflitoVersaoException` para conflito de versao.
- O teste de atomicidade usa uma constraint temporaria no PostgreSQL para forcar falha no lote; isso e bom para
  aprendizado, mas e um teste de infraestrutura um pouco mais acoplado ao banco.
- As tabelas `saldo_conta` e `extrato_conta` nao existem no migration do Liquibase; `SaldoRepository` e
  `ExtratoRepository` (implementacoes PostgreSQL) vao falhar em runtime ate que as migrations sejam criadas.
- `ConsultarExtratoHandler` existe mas esta vazio; precisa ser implementado.
- `SaldoProjection` e `ExtratoProjection` estao implementadas e testadas com mocks em memoria, mas nenhum mecanismo as
  chama apos eventos serem salvos; o wiring entre o fluxo de escrita e as projections ainda nao existe.
- ES2-08 (edge cases de persistencia) ainda esta parcialmente coberto; falta validar comportamento para payload
  invalido, eventType desconhecido e lacuna de versao no stream.

## Preferences

- Documentacao e requisitos em portugues.
- Projeto pequeno, didatico e incremental.
- Preferir requisitos de aprendizado arquitetural a novas funcionalidades bancarias.