# Roadmap

**Current Milestone:** Fase 2 - Persistencia de eventos
**Status:** In Progress

---

## Fase 1 - Dominio puro com Event Sourcing

**Goal:** Ter uma conta bancaria funcional em memoria, onde commands geram events, falhas de negocio sao valores, e o estado e sempre reconstruido por fold.
**Target:** Concluir quando todos os fluxos principais tiverem testes unitarios sem depender de Spring, banco ou Kafka.

### Features

**Conta Bancaria Event Sourced** - COMPLETE

- Abrir, bloquear e encerrar conta.
- Depositar, sacar e transferir dinheiro.
- Validar regras de saldo, conta encerrada e conta bloqueada.
- Retornar sucesso ou falha de command como valor de dominio.
- Reconstruir `ContaEstado` a partir de uma lista de eventos.

**Testes de comportamento do dominio** - COMPLETE

- Testar cada command pelo evento produzido.
- Testar replay de eventos.
- Testar rejeicoes de regras de negocio.
- Testar que falhas de negocio nao produzem eventos.

---

## Fase 2 - Persistencia de eventos

**Goal:** Persistir eventos em PostgreSQL e reconstruir aggregates a partir do event store.

### Features

**Event Store PostgreSQL** - IN PROGRESS

- Salvar eventos por aggregate. DONE baseline
- Carregar stream de eventos de uma conta. DONE baseline
- Controlar ordem e versao dos eventos. DONE baseline
- Aplicar concorrencia otimista com `expectedVersion`. DONE baseline
- Armazenar metadados minimos do evento. DONE baseline
- Serializar payload dos eventos em JSONB. DONE baseline
- Desserializar eventos persistidos para `ContaEvento`. DONE baseline
- Garantir atomicidade de lote quando append falha. DONE baseline

**Repositorio de Aggregate** - IN PROGRESS

- Reidratar `ContaBancaria` a partir do event store. IMPLEMENTED, needs tests
- Persistir novos eventos gerados por commands. IMPLEMENTED baseline, needs tests
- Rejeitar escrita quando a versao esperada nao bater com a versao persistida. COVERED in event store, needs aggregate-repository flow test

**Event Store em Memoria** - PLANNED

- Implementar a mesma porta do event store sem PostgreSQL.
- Apoiar testes de aplicacao sem subir Spring.
- Simular concorrencia otimista por versao.

---

## Fase 3 - Event Driven e CQRS

**Goal:** Publicar eventos no Kafka e criar read models separados do aggregate.

### Features

**Publicacao Kafka** - PLANNED

- Publicar eventos no topico `conta-eventos`.
- Serializar eventos com metadados minimos.
- Preservar `eventId`, `correlationId` e `causationId` para rastreabilidade.

**Projections CQRS** - PLANNED

- Projetar tabela `saldo_conta`.
- Projetar tabela `extrato`.
- Preparar consumidores de auditoria e notificacoes.
- Garantir idempotencia basica de consumers usando `eventId`.

---

## Fase 4 - Transferencia como processo

**Goal:** Evoluir transferencia para um processo entre aggregates, praticando consistencia eventual.

### Features

**Saga de Transferencia** - PLANNED

- Debitar conta origem.
- Creditar conta destino.
- Coordenar sucesso/falha por eventos.
- Registrar estado do processo de transferencia.

---

## Future Considerations

- API HTTP para envio de commands depois que o dominio e persistencia estiverem estaveis.
- Outbox pattern para consistencia entre banco e Kafka.
- Snapshot de aggregate.
- Idempotencia avancada de consumers.
- Observabilidade de eventos e correlation IDs.

## Immediate Next Challenges

- Criar testes unitarios para `ContaBancariaRepository` usando uma implementacao fake/in-memory da porta `EventStore`.
- Validar o fluxo completo: salvar eventos, buscar aggregate reidratado, processar novo command e persistir apenas os novos eventos.
- Decidir como a aplicacao calcula/carrega a `versaoAtual` ao salvar novos eventos depois da reidratacao.
- Considerar mover ou nomear melhor os pacotes de infraestrutura (`br.com.funcional.infra`) para manter a separacao didatica clara.
- Implementar `EventStore` em memoria para testes de aplicacao sem Spring.
