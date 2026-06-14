# Conta Bancaria Event Sourced Specification

## Problem Statement

O projeto precisa modelar uma conta bancaria pequena o suficiente para aprendizado, mas rica o bastante para praticar DDD, Event Sourcing, CQRS, arquitetura orientada a eventos e programacao funcional. A primeira entrega deve concentrar o aprendizado no dominio: commands, eventos imutaveis, erros como valores, validacoes no aggregate e estado reconstruido por replay.

## Goals

- [ ] Representar intencoes do usuario como commands.
- [ ] Representar fatos do dominio como events imutaveis.
- [ ] Reconstruir `ContaEstado` exclusivamente por `fold(eventos)`.
- [ ] Validar regras de negocio no aggregate antes de emitir eventos.
- [ ] Modelar sucesso e falha de command como valores de dominio.
- [ ] Manter a Fase 1 livre de Spring, banco de dados e Kafka.

## Out of Scope

| Feature | Reason |
| ------- | ------ |
| Kafka | Deve entrar somente depois que eventos e replay estiverem claros. |
| PostgreSQL | Persistencia distrai do primeiro objetivo: dominio puro. |
| API HTTP | Commands podem ser exercitados por testes unitarios na Fase 1. |
| Usuarios e autenticacao | Nao sao necessarios para estudar o aggregate. |
| Tarifas, juros e limites especiais | Aumentam o dominio sem ajudar no primeiro aprendizado. |
| Saga de transferencia | Deve ser estudada depois que transferencia simples e event store estiverem claros. |

---

## Domain Model

### Aggregate

`ContaBancaria`

Responsavel por receber commands, validar regras de negocio e produzir eventos.

### Estado reconstruido

```kotlin
data class ContaEstado(
    val id: UUID,
    val saldo: BigDecimal,
    val ativa: Boolean,
    val bloqueada: Boolean,
    val encerrada: Boolean
)
```

O estado nao deve ser salvo como fonte da verdade. Ele deve ser derivado:

```text
estado = fold(eventos)
```

### Resultado de command

Commands devem retornar um resultado explicito, permitindo exercitar FP e deixando as falhas de negocio testaveis sem depender de exceptions para fluxo esperado.

```kotlin
sealed interface ResultadoCommand

data class CommandAceito(
    val eventos: List<ContaEvento>
) : ResultadoCommand

data class CommandRejeitado(
    val erro: ErroDominio
) : ResultadoCommand
```

### Erros de dominio

```kotlin
sealed interface ErroDominio

data object ContaJaAberta : ErroDominio
data object ContaNaoAberta : ErroDominio
data object ContaEncerrada : ErroDominio
data object ContaBloqueada : ErroDominio
data object SaldoInsuficiente : ErroDominio
data object ValorInvalido : ErroDominio
data object ContaDestinoInvalida : ErroDominio
```

### Commands

- `AbrirConta`
- `Depositar`
- `Sacar`
- `Transferir`
- `BloquearConta`
- `EncerrarConta`

### Events

- `ContaAberta`
- `DinheiroDepositado`
- `DinheiroSacado`
- `TransferenciaRealizada`
- `ContaBloqueada`
- `ContaEncerrada`

### Evento persistido, a partir da Fase 2

Eventos de dominio continuam pequenos e focados no fato ocorrido. Quando houver persistencia ou publicacao, eles devem ser embrulhados em um envelope com metadados.

```kotlin
data class EventoPersistido(
    val eventId: UUID,
    val aggregateId: UUID,
    val aggregateVersion: Long,
    val occurredAt: Instant,
    val correlationId: UUID?,
    val causationId: UUID?,
    val payload: ContaEvento
)
```

### Decisoes didaticas

- Conta bloqueada nao aceita deposito, saque nem transferencia na Fase 1.
- Transferencia na Fase 1 e uma operacao da conta origem: valida saldo, emite evento e registra `contaDestinoId`.
- Creditar a conta destino por processo separado, saga ou process manager fica fora da Fase 1.

---

## User Stories

### P1: Abrir conta MVP

**User Story**: Como estudante do dominio, quero abrir uma conta bancaria para iniciar um stream de eventos valido.

**Why P1**: Sem conta aberta nao ha aggregate valido para as demais operacoes.

**Acceptance Criteria**:

1. WHEN o command `AbrirConta` for processado para uma conta inexistente THEN o sistema SHALL emitir `ContaAberta`.
2. WHEN `ContaAberta` for aplicado no fold THEN o sistema SHALL reconstruir uma conta ativa com saldo zero.
3. WHEN uma conta ja aberta receber novo `AbrirConta` THEN o sistema SHALL rejeitar o command com `ContaJaAberta`.

**Independent Test**: Executar `AbrirConta`, verificar evento gerado e reconstruir estado por fold.

---

### P1: Depositar dinheiro MVP

**User Story**: Como estudante do dominio, quero depositar dinheiro em uma conta ativa para observar evento e mudanca de estado via replay.

**Why P1**: Deposito e o fluxo mais simples para sentir a relacao command -> evento -> fold.

**Acceptance Criteria**:

1. WHEN `Depositar` for processado para uma conta ativa com valor positivo THEN o sistema SHALL emitir `DinheiroDepositado`.
2. WHEN `DinheiroDepositado` for aplicado no fold THEN o sistema SHALL aumentar o saldo anterior pelo valor do evento.
3. WHEN `Depositar` receber valor menor ou igual a zero THEN o sistema SHALL rejeitar o command com `ValorInvalido`.
4. WHEN a conta estiver encerrada THEN o sistema SHALL rejeitar o command com `ContaEncerrada`.
5. WHEN a conta estiver bloqueada THEN o sistema SHALL rejeitar o command com `ContaBloqueada`.
6. WHEN o historico nao contiver `ContaAberta` para o `idConta` do command THEN o sistema SHALL rejeitar o command com `ContaNaoAberta`.

**Independent Test**: Abrir conta, depositar 100, reconstruir estado e verificar saldo 100.

---

### P1: Sacar dinheiro MVP

**User Story**: Como estudante do dominio, quero sacar dinheiro de uma conta ativa para praticar validacao baseada no estado reconstruido.

**Why P1**: Saque introduz a regra essencial de saldo nao negativo.

**Acceptance Criteria**:

1. WHEN `Sacar` for processado para uma conta ativa com saldo suficiente THEN o sistema SHALL emitir `DinheiroSacado`.
2. WHEN `DinheiroSacado` for aplicado no fold THEN o sistema SHALL reduzir o saldo anterior pelo valor do evento.
3. WHEN `Sacar` deixar saldo negativo THEN o sistema SHALL rejeitar o command com `SaldoInsuficiente`.
4. WHEN `Sacar` receber valor menor ou igual a zero THEN o sistema SHALL rejeitar o command com `ValorInvalido`.
5. WHEN a conta estiver encerrada THEN o sistema SHALL rejeitar o command com `ContaEncerrada`.
6. WHEN a conta estiver bloqueada THEN o sistema SHALL rejeitar o command com `ContaBloqueada`.
7. WHEN o historico nao contiver `ContaAberta` para o `idConta` do command THEN o sistema SHALL rejeitar o command com `ContaNaoAberta`.

**Independent Test**: Abrir conta, depositar 100, sacar 30, reconstruir estado e verificar saldo 70.

---

### P1: Encerrar conta MVP

**User Story**: Como estudante do dominio, quero encerrar uma conta para garantir que eventos futuros respeitem o ciclo de vida do aggregate.

**Why P1**: A regra "conta encerrada nao aceita operacoes" protege todos os fluxos financeiros.

**Acceptance Criteria**:

1. WHEN `EncerrarConta` for processado para uma conta ativa THEN o sistema SHALL emitir `ContaEncerrada`.
2. WHEN `ContaEncerrada` for aplicado no fold THEN o sistema SHALL marcar a conta como encerrada e inativa.
3. WHEN uma conta encerrada receber `Depositar`, `Sacar`, `Transferir`, `BloquearConta` ou `EncerrarConta` THEN o sistema SHALL rejeitar o command com `ContaEncerrada`.
4. WHEN `EncerrarConta` for processado em conta bloqueada THEN o sistema SHALL permitir o encerramento.

**Independent Test**: Abrir conta, encerrar conta, tentar depositar e verificar rejeicao.

---

### P1: Resultado de command e erros de dominio

**User Story**: Como estudante de FP, quero que commands retornem sucesso ou falha como valores para tratar regras de negocio de forma explicita.

**Why P1**: Esse requisito melhora o aprendizado de programacao funcional sem aumentar o dominio bancario.

**Acceptance Criteria**:

1. WHEN um command for aceito THEN o sistema SHALL retornar `CommandAceito` com uma lista nao vazia de eventos.
2. WHEN um command for rejeitado THEN o sistema SHALL retornar `CommandRejeitado` com um `ErroDominio`.
3. WHEN um command for rejeitado THEN o sistema SHALL nao produzir eventos.
4. WHEN ocorrer uma falha esperada de negocio THEN o sistema SHALL nao depender de exception para representar a rejeicao.

**Independent Test**: Tentar sacar sem saldo e verificar `CommandRejeitado(SaldoInsuficiente)` sem eventos.

---

### P2: Bloquear conta

**User Story**: Como estudante do dominio, quero bloquear uma conta para diferenciar conta existente, ativa e operacional.

**Why P2**: Bloqueio adiciona uma variacao de estado util, mas pode vir depois do ciclo principal abrir/depositar/sacar/encerrar.

**Acceptance Criteria**:

1. WHEN `BloquearConta` for processado para uma conta ativa e nao encerrada THEN o sistema SHALL emitir `ContaBloqueada`.
2. WHEN `ContaBloqueada` for aplicado no fold THEN o sistema SHALL marcar a conta como bloqueada.
3. WHEN a conta estiver bloqueada THEN o sistema SHALL rejeitar `Depositar`, `Sacar` e `Transferir`.
4. WHEN a conta estiver encerrada THEN o sistema SHALL rejeitar `BloquearConta` com `ContaEncerrada`.
5. WHEN a conta ja estiver bloqueada THEN o sistema SHALL rejeitar novo `BloquearConta` com `ContaBloqueada`.

**Independent Test**: Abrir conta, bloquear conta, tentar sacar e verificar rejeicao.

---

### P2: Transferir dinheiro

**User Story**: Como estudante do dominio, quero transferir dinheiro entre contas para praticar uma regra que depende de saldo da conta origem.

**Why P2**: Transferencia e mais rica que saque, mas levanta decisoes sobre um ou dois aggregates. Para Fase 1, ela deve ser mantida simples e didatica.

**Acceptance Criteria**:

1. WHEN `Transferir` for processado com origem ativa, destino informado e saldo suficiente THEN o sistema SHALL emitir `TransferenciaRealizada`.
2. WHEN `TransferenciaRealizada` for aplicado no fold da conta origem THEN o sistema SHALL reduzir o saldo pelo valor transferido.
3. WHEN a conta origem nao tiver saldo suficiente THEN o sistema SHALL rejeitar o command com `SaldoInsuficiente`.
4. WHEN `Transferir` receber valor menor ou igual a zero THEN o sistema SHALL rejeitar o command com `ValorInvalido`.
5. WHEN a conta origem estiver encerrada ou bloqueada THEN o sistema SHALL rejeitar o command.
6. WHEN a conta destino for igual a conta origem THEN o sistema SHALL rejeitar o command com `ContaDestinoInvalida`.
7. WHEN o historico nao contiver `ContaAberta` para a conta origem THEN o sistema SHALL rejeitar o command com `ContaNaoAberta`.

**Independent Test**: Abrir conta origem, depositar 100, transferir 40 para uma conta destino identificada por UUID e verificar saldo origem 60 por replay.

---

### P3: Read models em memoria

**User Story**: Como estudante de CQRS, quero projetar eventos em modelos de leitura simples para entender a separacao entre escrita e consulta.

**Why P3**: CQRS fica mais concreto apos o aggregate e os eventos estarem funcionando.

**Acceptance Criteria**:

1. WHEN eventos financeiros forem projetados THEN o sistema SHALL atualizar um read model `saldo_conta`.
2. WHEN eventos financeiros forem projetados THEN o sistema SHALL adicionar linhas em um read model `extrato`.
3. WHEN os eventos forem reprocessados desde o inicio THEN os read models SHALL produzir o mesmo resultado.

---

### P3: Metadados e versionamento de eventos

**User Story**: Como estudante de Event Sourcing, quero preparar eventos persistidos com metadados para entender rastreabilidade, ordenacao e concorrencia.

**Why P3**: Esses conceitos ficam mais importantes na Fase 2, mas devem estar previstos para evitar redesenho grande.

**Acceptance Criteria**:

1. WHEN um evento for persistido THEN o sistema SHALL associar `eventId`, `aggregateId`, `aggregateVersion` e `occurredAt`.
2. WHEN um command for persistido na Fase 2 THEN o sistema SHALL validar `expectedVersion` contra a versao atual do stream.
3. WHEN `expectedVersion` divergir da versao atual THEN o sistema SHALL rejeitar a escrita por conflito de concorrencia.
4. WHEN eventos forem publicados na Fase 3 THEN o sistema SHALL preservar `correlationId` e `causationId` quando disponiveis.

---

### P3: Consumers idempotentes

**User Story**: Como estudante de Event Driven, quero que projections processem eventos de forma idempotente para evitar duplicidade quando mensagens forem reentregues.

**Why P3**: Idempotencia e uma das primeiras dores reais ao usar Kafka e projections.

**Acceptance Criteria**:

1. WHEN uma projection receber o mesmo `eventId` mais de uma vez THEN o sistema SHALL aplicar o evento apenas uma vez.
2. WHEN a projection for reconstruida do zero THEN o sistema SHALL poder reprocessar todos os eventos em ordem.
3. WHEN a projection falhar durante processamento THEN o sistema SHALL permitir retomada sem corromper `saldo_conta` ou `extrato`.

---

## Edge Cases

- WHEN qualquer command financeiro for executado antes de `ContaAberta` THEN o sistema SHALL rejeitar o command.
- WHEN um helper de validacao encontrar erro de dominio THEN o aggregate SHALL retornar esse erro sem continuar processando o command.
- WHEN qualquer command financeiro receber valor menor ou igual a zero THEN o sistema SHALL rejeitar o command.
- WHEN qualquer operacao for tentada em conta encerrada THEN o sistema SHALL rejeitar o command.
- WHEN deposito, saque ou transferencia forem tentados em conta bloqueada THEN o sistema SHALL rejeitar o command.
- WHEN saque ou transferencia exceder o saldo disponivel THEN o sistema SHALL rejeitar o command.
- WHEN transferencia tiver origem e destino iguais THEN o sistema SHALL rejeitar o command.
- WHEN command for rejeitado THEN o sistema SHALL nao produzir eventos.
- WHEN command for aceito THEN o novo estado SHALL ser verificavel por replay de `eventosHistoricos + novosEventos`.
- WHEN eventos forem aplicados em ordem diferente da ordem original THEN o sistema SHALL nao assumir consistencia do estado.
- WHEN dinheiro for modelado THEN o sistema SHALL usar `BigDecimal` ou value object equivalente, nao `Double`.

---

## Requirement Traceability

| Requirement ID | Story | Phase | Status |
| -------------- | ----- | ----- | ------ |
| BANCO-01 | P1: Abrir conta | Execute | Verified |
| BANCO-02 | P1: Depositar dinheiro | Execute | Verified |
| BANCO-03 | P1: Sacar dinheiro | Execute | Verified |
| BANCO-04 | P1: Encerrar conta | Execute | Verified |
| BANCO-05 | P1: Resultado de command e erros de dominio | Execute | Verified |
| BANCO-06 | P2: Bloquear conta | Execute | Verified |
| BANCO-07 | P2: Transferir dinheiro | Execute | Verified |
| BANCO-08 | P3: Read models em memoria | Specify | Pending |
| BANCO-09 | Estado reconstruido por fold | Execute | Verified |
| BANCO-10 | Edge cases financeiros | Execute | Verified |
| BANCO-11 | Metadados e versionamento de eventos | Specify | Pending |
| BANCO-12 | Consumers idempotentes | Specify | Pending |

**Coverage:** 12 total, 9 verified for Fase 1 scope, 3 pending for later phases.

---

## Success Criteria

- [x] Todos os commands P1 possuem testes que validam eventos produzidos.
- [x] Todos os eventos P1 alteram estado apenas via reducer/fold.
- [x] Testes de commands aceitos validam o novo estado por replay de eventos antigos somados aos eventos novos.
- [x] Regras de saldo, conta encerrada e valores invalidos possuem testes de rejeicao.
- [x] Commands rejeitados retornam erro de dominio e nao produzem eventos.
- [x] Nenhum teste de Fase 1 precisa subir contexto Spring.
- [x] O modelo de dinheiro nao usa `Double`.
