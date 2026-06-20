# Roadmap

**Current Milestone:** Fase 3 - Event Driven e CQRS
**Status:** Not Started

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
**Status:** COMPLETE

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
- Detectar lacuna de versao no stream e lancar `EventoInconsistenteException`. DONE
- Rejeitar payload invalido com `PayloadInvalidoException` no mapper. DONE
- Rejeitar `eventType` desconhecido com erro claro. DONE

**Repositorio de Aggregate** - DONE

- Reidratar `ContaBancaria` a partir do event store. DONE
- Persistir novos eventos gerados por commands. DONE
- Rejeitar escrita quando a versao esperada nao bater com a versao persistida. DONE
- Testes de reidratacao e persistencia com `InMemoryEventStore`. DONE

**Event Store em Memoria** - DONE

- Implementar a mesma porta do event store sem PostgreSQL. DONE
- Apoiar testes de aplicacao sem subir Spring. DONE
- Simular concorrencia otimista por versao. DONE

**Metadados de rastreabilidade** - DONE

- `causationId` implementado via `commandId` gerado no proprio command. DONE
- `correlationId` adiado para Fase 3; sera fornecido pela borda HTTP ou Kafka consumer. DEFERRED

---

## Fase 3 - Event Driven e CQRS

**Goal:** Publicar eventos no Kafka e criar read models separados do aggregate.
**Status:** Not Started

### Features

**Migrations de Read Models** - PENDING (prerequisito para iniciar Fase 3)

- Criar tabela `saldo_conta` no Liquibase. PENDING
- Criar tabela `extrato_conta` no Liquibase. PENDING

**Mecanismo de Wiring entre Event Store e Projections** - PENDING

- Definir dispatcher de eventos apos persistencia (ApplicationEvent do Spring ou dispatcher simples). PENDING
- Conectar `SaldoProjection` ao fluxo de escrita. PENDING
- Conectar `ExtratoProjection` ao fluxo de escrita. PENDING
- Implementar `ConsultarExtratoHandler`. PENDING

**Publicacao Kafka** - PLANNED

- Publicar eventos no topico `conta-eventos`.
- Serializar eventos com metadados minimos.
- Preservar `causationId` e futuramente `correlationId` para rastreabilidade.

**Projections CQRS** - PARTIALLY IMPLEMENTED

- Projetar tabela `saldo_conta`. LOGIC DONE, needs wiring and migration
- Projetar tabela `extrato_conta`. LOGIC DONE, needs wiring and migration
- Implementar `ConsultarExtratoHandler`. PENDING
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
- `correlationId` vindo do header HTTP ou Kafka na Fase 3.
- Outbox pattern para consistencia entre banco e Kafka.
- Snapshot de aggregate.
- Idempotencia avancada de consumers.
- Observabilidade de eventos e correlation IDs.

## Immediate Next Challenges

- Criar migrations Liquibase para `saldo_conta` e `extrato_conta` — prerequisito obrigatorio para Fase 3.
- Definir e implementar mecanismo de wiring entre evento salvo e projections antes de introduzir Kafka.
- Implementar `ConsultarExtratoHandler`.
- Introduzir `correlationId` vindo da borda HTTP quando o controller for criado.