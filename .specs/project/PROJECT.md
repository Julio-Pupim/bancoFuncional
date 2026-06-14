# Mini Banco Digital

**Vision:** Construir um mini banco digital para aprender DDD, Event Sourcing, CQRS, arquitetura orientada a eventos e programacao funcional em Kotlin, comecando pelo dominio puro antes de adicionar infraestrutura.
**For:** Desenvolvedor estudando arquitetura de software aplicada em um dominio pequeno.
**Solves:** Da um ambiente controlado para praticar imutabilidade, reducers, replay de eventos, erros como valores e separacao entre escrita e leitura sem a complexidade de um banco real.

## Goals

- Modelar uma `ContaBancaria` como aggregate com commands, events e estado reconstruido por `fold`.
- Implementar primeiro o dominio puro em Kotlin, com testes que demonstrem abertura de conta, deposito, saque, transferencia, bloqueio, encerramento e rejeicoes de negocio.
- Praticar FP modelando sucesso e falha de commands como valores, sem usar exceptions para fluxo esperado do dominio.
- Evoluir em fases para persistencia de eventos, publicacao em Kafka e projections CQRS sem perder a clareza do dominio.

## Tech Stack

**Core:**

- Framework: Spring Boot 4.0.6, adiado para fases de infraestrutura
- Language: Kotlin 2.2.21
- Database: PostgreSQL, apenas a partir da Fase 2

**Key dependencies:**

- Kotlin standard library
- JUnit 5 / kotlin-test
- Spring Data JDBC, a partir da Fase 2
- Spring Kafka, a partir da Fase 3
- Jackson Kotlin, quando eventos precisarem ser serializados

## Scope

**v1 includes:**

- Dominio puro de conta bancaria.
- Commands e events imutaveis.
- Erros de dominio modelados explicitamente.
- Regras de negocio executadas no aggregate.
- Estado reconstruido por replay/fold dos eventos.
- Testes unitarios focados em comportamento do dominio.

**Explicitly out of scope:**

- API HTTP.
- Banco de dados.
- Kafka.
- Autenticacao, usuarios, senhas ou seguranca real.
- Tarifas, juros, estorno, agendamento, limites especiais ou compliance bancario.
- Transferencia distribuida entre duas contas como saga/process manager.

## Constraints

- Technical: Fase 1 deve evitar Spring, PostgreSQL e Kafka para priorizar aprendizado de DDD, Event Sourcing e FP.
- Learning: cada fase deve introduzir uma ideia arquitetural por vez.
- Domain: dinheiro deve ser modelado com tipo seguro, preferencialmente `BigDecimal`, evitando `Double`.
- Domain: transferencia na Fase 1 deve ser uma operacao didatica na conta origem; transferencia distribuida fica para fase futura.
