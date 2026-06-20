# Roadmap

**Current Milestone:** Fase 2 - Persistencia de eventos
**Status:** In Progress

---

## Fase 1 - Dominio puro com Event Sourcing

**Goal:** Ter uma conta bancaria funcional em memoria, onde commands geram events, falhas de negocio sao valores, e o
estado e sempre reconstruido por fold.
**Status:** COMPLETE

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
**Status:** In Progress

### Features

**Event Store PostgreSQL** - DONE

- Salvar eventos por aggregate. DONE
- Carregar stream de eventos de uma conta. DONE
- Controlar ordem e versao dos eventos. DONE
- Aplicar concorrencia otimista com `expectedVersion`. DONE
- Armazenar metadados minimos do evento. DONE
- Serializar payload dos eventos em JSONB. DONE
- Desserializar eventos persistidos para `ContaEvento`. DONE
- Garantir atomicidade de lote quando append falha. DONE

**Repositorio de Aggregate** - DONE

- Reidratar `ContaBancaria` a partir do event store. DONE
- Persistir novos eventos gerados por commands. DONE
- Rejeitar escrita quando a versao esperada nao bater com a versao persistida. DONE
- Testes de reidratacao e persistencia com `InMemoryEventStore`. DONE

**Event Store em Memoria** - DONE

- Implementar a mesma porta do event store sem PostgreSQL. DONE
- Apoiar testes de aplicacao sem subir Spring. DONE
- Simular concorrencia otimista por versao. DONE

**Metadados de rastreabilidade** - IN PROGRESS

- Definir campos `correlationId` e `causationId` no `EventoPersistido`. PENDING
- Persistir metadados opcionais no event store. PENDING
- Disponibilizar metadados para publicacao futura no Kafka. PENDING

**Edge Cases de persistencia** - IN PROGRESS

- Rejeitar payload invalido com erro explicito. PENDING
- Rejeitar `eventType` desconhecido com erro claro. PENDING
- Tratar lacuna de versao no stream como stream inconsistente. PENDING

---

## Fase 3 - Event Driven e CQRS

**Goal:** Publicar eventos no Kafka e criar read models separados do aggregate.

### Features

**Migrations de Read Models** - PENDING (prerequisito para iniciar Fase 3)

- Criar tabela `saldo_conta` no Liquibase. PENDING
- Criar tabela `extrato_conta` no Liquibase. PENDING

**Publicacao Kafka** - PLANNED

- Publicar eventos no topico `conta-eventos`.
- Serializar eventos com metadados minimos.
- Preservar `eventId`, `correlationId` e `causationId` para rastreabilidade.

**Projections CQRS** - PARTIALLY IMPLEMENTED

- Projetar tabela `saldo_conta`. LOGIC DONE, needs wiring and migration
- Projetar tabela `extrato_conta`. LOGIC DONE, needs wiring and migration
- Implementar `ConsultarExtratoHandler`. PENDING
- Criar mecanismo de wiring entre evento salvo e projections. PENDING
- Preparar consumidores de auditoria e notificacoes. PLANNED
- Garantir idempotencia basica de consumers usando `eventId`. PLANNED

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

- Concluir ES2-07: definir e persistir `correlationId` e `causationId` no `EventoPersistido` e na tabela `event_store`.
- Concluir  : cobrir edge cases de payload invalido, `eventType` desconhecido e lacuna de versao no stream.
- Criar migrations Liquibase para `saldo_conta` e `extrato_conta` como prerequisito da Fase 3.
- Implementar `ConsultarExtratoHandler`.
- Definir mecanismo de wiring entre evento salvo e projections (ApplicationEvent do Spring ou dispatcher simples) antes
  de introduzir Kafka.