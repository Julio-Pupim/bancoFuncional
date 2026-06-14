# Project State

## Decisions

- O projeto sera usado principalmente para aprendizado de DDD, Event Sourcing, CQRS, Event Driven e FP.
- A Fase 1 deve ser intencionalmente sem Kafka, banco de dados e Spring.
- O estado da conta nao sera a fonte da verdade; ele sera reconstruido por `fold(eventos)`.
- Eventos sao fatos imutaveis e commands representam intencao do usuario.
- Falhas esperadas de negocio devem ser modeladas como valores de dominio, nao como exceptions usadas para fluxo normal.
- Conta bloqueada, para fins didaticos da Fase 1, nao aceita deposito, saque nem transferencia.
- Transferencia na Fase 1 afeta apenas a conta origem e registra o destino por identificador; saga/process manager fica para uma fase posterior.
- Na Fase 2, stream inexistente no event store deve ser tratado como versao `0`.
- `EventStoreRepository` deve lidar com registros persistidos (`EventoPersistido`), enquanto a conversao para `ContaEvento` fica em um mapper separado.
- O payload fica como `jsonb` no PostgreSQL, mas volta para Kotlin como `String` JSON via `payload::text`.
- `append` deve ser transacional: se uma chamada tenta gravar multiplos eventos e algum falha, nenhum evento daquela chamada deve permanecer gravado.

## Current Focus

- Fase 1 concluida: dominio puro de `ContaBancaria` com commands, events, erros como valores e replay/fold.
- Fase 2 em execucao: `EventStoreRepository.append` e `load` existem sobre PostgreSQL/Testcontainers.
- `append` ja grava eventos com versoes sequenciais, trata stream vazio como versao `0`, usa `@Transactional` e valida versao esperada contra `max(aggregate_version)`.
- `load` ja retorna `List<EventoPersistido>`, filtra por `aggregateId`, ordena por `aggregate_version` e le `payload` como JSON textual.
- `EventoMapper` foi separado de `EventoPersistido` e converte todos os subtipos atuais de `ContaEvento`.
- A porta `EventStore` foi criada e `EventStoreRepository` passou a implementa-la.
- `ContaBancariaRepository` foi criado para carregar `EventoPersistido`, mapear para `ContaEvento` e reidratar `ContaBancaria`.
- Proximo foco recomendado: criar testes para `ContaBancariaRepository` e decidir como representar a versao atual do aggregate reidratado para salvar novos eventos.
- Refatoracoes de validacao no aggregate devem preservar retorno imediato de erro; helper que retorna `CommandRejeitado?` nao pode ser chamado e ignorado.
- Validacoes comuns do aggregate foram separadas em helpers com responsabilidades diferentes: pergunta booleana (`contaExisteNoHistorico`) e validacoes que retornam `CommandRejeitado?`.
- Como erros de dominio e eventos podem ter nomes iguais (`ContaBloqueada`, `ContaEncerrada`), o codigo pode usar aliases para deixar claro quando esta falando do evento.

## Deferred

- Kafka, consumers e projections ficam para a Fase 3.
- Transferencia distribuida entre duas contas fica para a Fase 4.
- API HTTP fica fora do primeiro escopo.

## Concerns

- O modelo de dinheiro ja usa `BigDecimal`; manter cuidado nos testes para evitar `BigDecimal(100.00)` e preferir strings como `BigDecimal("100.00")`.
- O `pom.xml` ja inclui Spring, JDBC e Kafka, mas isso nao deve dirigir a Fase 1.
- O envelope minimo de evento ja existe em `EventoPersistido`, mas `correlationId` e `causationId` continuam adiados.
- O reducer/fold de eventos financeiros deve transformar o estado anterior. `DinheiroDepositado(10)` significa somar 10 ao saldo atual, nao definir saldo como 10.
- O helper atual de conta aberta pode mascarar erro se o chamador nao retornar seu resultado; teste de deposito sem conta aberta cobre esse risco.
- A suite de dominio cobre cenarios de id de conta incorreto, transferencia para mesma conta, transferencia valida, bloqueio/encerramento sem historico, conta bloqueada e conta encerrada.
- A suite de dominio possui 33 testes unitarios sem subir Spring.
- `EventStoreRepositoryTest` possui 6 testes de integracao passando para append/load, versao incorreta, multiplos eventos, atomicidade de lote, stream vazio e filtro/ordem por aggregate.
- `EventoMapperTest` possui 6 testes unitarios passando, cobrindo todos os subtipos atuais de `ContaEvento`.
- Em 2026-06-06, `mvnw test` passou com 45 testes: 33 de dominio, 6 de mapper e 6 de event store PostgreSQL.
- `EventStoreRepository.append` ja lanca `ConflitoVersaoException` para conflito de versao.
- `ContaBancariaRepository` ainda nao possui testes proprios; sua reidratacao existe no codigo, mas ainda precisa ser verificada por teste.
- O teste de atomicidade usa uma constraint temporaria no PostgreSQL para forcar falha no lote; isso e bom para aprendizado, mas e um teste de infraestrutura um pouco mais acoplado ao banco.

## Preferences

- Documentacao e requisitos em portugues.
- Projeto pequeno, didatico e incremental.
- Preferir requisitos de aprendizado arquitetural a novas funcionalidades bancarias.
