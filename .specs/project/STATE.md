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
- `correlationId` sera introduzido na Fase 3, quando a borda do sistema (HTTP controller ou Kafka consumer) puder
  fornecer o valor externo. Commands carregam `causationId` via `commandId` gerado no proprio command.
- Validacao de integridade do stream (lacuna de versao) e responsabilidade do `EventStoreRepository.load()`, pois
  qualquer aggregate que use o event store se beneficia da protecao sem duplicar logica.
- Excecoes especificas de infraestrutura do event store ficam em `domain/exception` para nao vazar detalhes de
  implementacao para as camadas superiores.

## Current Focus

- Fase 1 concluida: dominio puro de `ContaBancaria` com commands, events, erros como valores e replay/fold.
- Fase 2 concluida: persistencia de eventos em PostgreSQL, reidratacao de aggregate, concorrencia otimista, serializacao
  JSON, event store em memoria, metadados de rastreabilidade e edge cases de persistencia.
- `EventStoreRepository.append` e `load` funcionam sobre PostgreSQL/Testcontainers com todas as garantias de
  integridade.
- `load` valida lacuna de versao via metodo privado `validarIntegridade`, lanca `EventoInconsistenteException` se
  detectar inconsistencia.
- `EventoMapper` captura excecoes do Jackson e relanca como `PayloadInvalidoException`, isolando o detalhe de
  serializacao.
- `causationId` implementado via `commandId` nos commands; `correlationId` adiado para quando a borda HTTP/Kafka
  existir.
- `SaldoProjection` e `ExtratoProjection` estao implementadas e testadas com mocks em memoria, mas nenhum mecanismo as
  chama apos eventos serem salvos.
- `ConsultarExtratoHandler` existe mas esta vazio.
- As tabelas `saldo_conta` e `extrato_conta` nao existem no migration do Liquibase.
- Fase 3 e o proximo foco: migrations de read models, mecanismo de wiring entre evento salvo e projections, e publicacao
  no Kafka.

## Deferred

- `correlationId` adiado para Fase 3; sera fornecido pela borda HTTP ou Kafka consumer.
- Kafka, consumers e wiring de projections ficam para a Fase 3.
- Transferencia distribuida entre duas contas fica para a Fase 4.
- API HTTP fica fora do primeiro escopo.

## Concerns

- O modelo de dinheiro ja usa `BigDecimal`; manter cuidado nos testes para evitar `BigDecimal(100.00)` e preferir
  strings como `BigDecimal("100.00")`.
- As tabelas `saldo_conta` e `extrato_conta` nao existem no migration do Liquibase; `SaldoRepository` e
  `ExtratoRepository` vao falhar em runtime ate que as migrations sejam criadas. Esse e o prerequisito mais urgente para
  iniciar a Fase 3.
- `ConsultarExtratoHandler` existe mas esta vazio; precisa ser implementado antes de expor a query de extrato.
- `SaldoProjection` e `ExtratoProjection` estao implementadas e testadas com mocks, mas nenhum mecanismo as chama apos
  eventos serem salvos; o wiring entre escrita e projections precisa ser definido antes do Kafka.
- O teste de atomicidade e o teste de lacuna de versao usam manipulacao direta do banco via `jdbcTemplate`; sao testes
  de infraestrutura acoplados ao schema e devem ser revisados se o schema mudar.

## Preferences

- Documentacao e requisitos em portugues.
- Projeto pequeno, didatico e incremental.
- Preferir requisitos de aprendizado arquitetural a novas funcionalidades bancarias.