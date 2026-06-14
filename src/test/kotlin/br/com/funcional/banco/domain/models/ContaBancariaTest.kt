package br.com.funcional.banco.domain.models

import br.com.funcional.banco.domain.commands.*
import br.com.funcional.banco.domain.eventos.ContaAberta
import br.com.funcional.banco.domain.eventos.DinheiroDepositado
import br.com.funcional.banco.domain.eventos.DinheiroSacado
import br.com.funcional.banco.domain.eventos.TransferenciaRealizada
import java.math.BigDecimal
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import br.com.funcional.banco.domain.eventos.ContaBloqueada as ContaBloqueadaEvento
import br.com.funcional.banco.domain.eventos.ContaEncerrada as ContaEncerradaEvento

class ContaBancariaTest {
    @Test
    fun `abrir conta sem eventos anteriores gera evento ContaAberta`() {
        val contaId = UUID.randomUUID()
        val aggregate = ContaBancaria()

        val resultado = aggregate.processar(
            AbrirConta(
                idConta = contaId,
                saldoInicial = BigDecimal.ZERO
            )
        )

        val commandAceito = assertIs<CommandAceito>(resultado)
        assertEquals(
            listOf(ContaAberta(contaId, BigDecimal.ZERO)),
            commandAceito.eventos
        )
    }

    @Test
    fun `estado da conta aberta nasce do fold dos eventos`() {
        val contaId = UUID.randomUUID()
        val eventos = listOf(ContaAberta(contaId, BigDecimal.TEN))

        val estado = ContaBancaria.reconstruirEstado(eventos)

        assertEquals(
            ContaEstado(
                id = contaId,
                saldo = BigDecimal.TEN,
                ativa = true,
                bloqueada = false,
                encerrada = false
            ),
            estado
        )
    }

    @Test
    fun `abrir conta ja aberta retorna erro de dominio`() {
        val contaId = UUID.randomUUID()
        val aggregate = ContaBancaria(
            eventosHistoricos = listOf(ContaAberta(contaId, BigDecimal.ZERO))
        )

        val resultado = aggregate.processar(
            AbrirConta(
                idConta = contaId,
                saldoInicial = BigDecimal.ZERO
            )
        )

        val commandRejeitado = assertIs<CommandRejeitado>(resultado)
        assertEquals(ContaJaAberta, commandRejeitado.erro)
    }

    @Test
    fun `realizar deposito em conta ja aberta`() {
        val contaId = UUID.randomUUID()
        val aggregate = ContaBancaria(eventosHistoricos = listOf(ContaAberta(contaId, BigDecimal.ZERO)))
        val resultado = aggregate.processar(
            Depositar(BigDecimal.TEN, contaId)
        )
        val commandAceito = assertIs<CommandAceito>(resultado)
        assertEquals(listOf(DinheiroDepositado(BigDecimal.TEN, contaId)), commandAceito.eventos)

    }

    @Test
    fun `realizar deposito em conta sem eventos anteriores`() {
        val contaId = UUID.randomUUID()
        val aggregate = ContaBancaria()
        val resultado = aggregate.processar(
            Depositar(
                BigDecimal.TEN,
                contaId
            )
        )
        val commandRejeitado = assertIs<CommandRejeitado>(resultado)
        assertEquals(
            ContaNaoAberta,
            commandRejeitado.erro
        )
    }

    @Test
    fun `realizar deposito em conta com outro deposito`() {
        val contaId = UUID.randomUUID()
        val aggregate = ContaBancaria(
            eventosHistoricos = listOf(
                ContaAberta(contaId, BigDecimal.ZERO),
                DinheiroDepositado(BigDecimal.TEN, contaId)
            )
        )
        val resultado = aggregate.processar(
            Depositar(BigDecimal.TEN, contaId)
        )
        val commandAceito = assertIs<CommandAceito>(resultado)
        assertEquals(listOf(DinheiroDepositado(BigDecimal.TEN, contaId)), commandAceito.eventos)
        val contaEstadoAtual = ContaBancaria.reconstruirEstado(
            listOf(
                ContaAberta(contaId, BigDecimal.ZERO),
                DinheiroDepositado(BigDecimal.TEN, contaId)
            ) + commandAceito.eventos
        )
        assertEquals(ContaEstado(contaId, BigDecimal.valueOf(20), true, false, false), contaEstadoAtual)
    }

    @Test
    fun `realizar saque em conta `() {
        val contaId = UUID.randomUUID()
        val aggregate = ContaBancaria(
            eventosHistoricos = listOf(
                ContaAberta(contaId, BigDecimal.ZERO),
                DinheiroDepositado(BigDecimal.valueOf(100), contaId)
            )
        )
        val resultado = aggregate.processar(Sacar(BigDecimal.TEN, contaId))
        val commandAceito = assertIs<CommandAceito>(resultado)
        assertEquals(listOf(DinheiroSacado(BigDecimal.TEN, contaId)), commandAceito.eventos)
        val contaEstadoAtual = ContaBancaria.reconstruirEstado(
            listOf(
                ContaAberta(contaId, BigDecimal.ZERO),
                DinheiroDepositado(BigDecimal.valueOf(100), contaId)
            ) + commandAceito.eventos
        )
        assertEquals(ContaEstado(contaId, BigDecimal.valueOf(90), true, false, false), contaEstadoAtual)
    }

    @Test
    fun `deposito para conta diferente do historico retorna conta nao aberta`() {
        val contaId = UUID.randomUUID()
        val outraContaId = UUID.randomUUID()
        val aggregate = ContaBancaria(eventosHistoricos = listOf(ContaAberta(contaId, BigDecimal.ZERO)))

        val resultado = aggregate.processar(Depositar(BigDecimal.TEN, outraContaId))

        val commandRejeitado = assertIs<CommandRejeitado>(resultado)
        assertEquals(ContaNaoAberta, commandRejeitado.erro)
    }

    @Test
    fun `saque para conta diferente do historico retorna conta nao aberta`() {
        val contaId = UUID.randomUUID()
        val outraContaId = UUID.randomUUID()
        val aggregate = ContaBancaria(
            eventosHistoricos = listOf(
                ContaAberta(contaId, BigDecimal.ZERO),
                DinheiroDepositado(BigDecimal.valueOf(100), contaId)
            )
        )

        val resultado = aggregate.processar(Sacar(BigDecimal.TEN, outraContaId))

        val commandRejeitado = assertIs<CommandRejeitado>(resultado)
        assertEquals(ContaNaoAberta, commandRejeitado.erro)
    }

    @Test
    fun `transferencia para a mesma conta retorna destino invalido`() {
        val contaId = UUID.randomUUID()
        val aggregate = ContaBancaria(
            eventosHistoricos = listOf(
                ContaAberta(contaId, BigDecimal.ZERO),
                DinheiroDepositado(BigDecimal.valueOf(100), contaId)
            )
        )

        val resultado = aggregate.processar(Transferir(BigDecimal.TEN, contaId, contaId))

        val commandRejeitado = assertIs<CommandRejeitado>(resultado)
        assertEquals(ContaDestinoInvalida, commandRejeitado.erro)
    }

    @Test
    fun `transferencia para outra conta gera evento e reduz saldo da origem no replay`() {
        val contaOrigemId = UUID.randomUUID()
        val contaDestinoId = UUID.randomUUID()
        val eventosHistoricos = listOf(
            ContaAberta(contaOrigemId, BigDecimal.ZERO),
            DinheiroDepositado(BigDecimal.valueOf(100), contaOrigemId)
        )
        val aggregate = ContaBancaria(eventosHistoricos = eventosHistoricos)

        val resultado = aggregate.processar(Transferir(BigDecimal.TEN, contaOrigemId, contaDestinoId))

        val commandAceito = assertIs<CommandAceito>(resultado)
        assertEquals(
            listOf(TransferenciaRealizada(BigDecimal.TEN, contaOrigemId, contaDestinoId)),
            commandAceito.eventos
        )
        assertEquals(
            ContaEstado(contaOrigemId, BigDecimal.valueOf(90), true, false, false),
            ContaBancaria.reconstruirEstado(eventosHistoricos + commandAceito.eventos)
        )
    }

    @Test
    fun `transferencia sem conta origem aberta retorna conta nao aberta`() {
        val contaOrigemId = UUID.randomUUID()
        val contaDestinoId = UUID.randomUUID()
        val aggregate = ContaBancaria()

        val resultado = aggregate.processar(Transferir(BigDecimal.TEN, contaOrigemId, contaDestinoId))

        val commandRejeitado = assertIs<CommandRejeitado>(resultado)
        assertEquals(ContaNaoAberta, commandRejeitado.erro)
    }

    @Test
    fun `bloquear conta sem historico retorna conta nao aberta`() {
        val contaId = UUID.randomUUID()
        val aggregate = ContaBancaria()

        val resultado = aggregate.processar(BloquearConta(contaId))

        val commandRejeitado = assertIs<CommandRejeitado>(resultado)
        assertEquals(ContaNaoAberta, commandRejeitado.erro)
    }

    @Test
    fun `encerrar conta sem historico retorna conta nao aberta`() {
        val contaId = UUID.randomUUID()
        val aggregate = ContaBancaria()

        val resultado = aggregate.processar(EncerrarConta(contaId))

        val commandRejeitado = assertIs<CommandRejeitado>(resultado)
        assertEquals(ContaNaoAberta, commandRejeitado.erro)
    }

    @Test
    fun `conta encerrada fica inativa no replay`() {
        val contaId = UUID.randomUUID()

        val estado = ContaBancaria.reconstruirEstado(
            listOf(
                ContaAberta(contaId, BigDecimal.ZERO),
                ContaEncerradaEvento(contaId)
            )
        )

        assertEquals(ContaEstado(contaId, BigDecimal.ZERO, false, false, true), estado)
    }

    @Test
    fun `deposito em conta bloqueada retorna conta bloqueada`() {
        val contaId = UUID.randomUUID()
        val aggregate = ContaBancaria(
            eventosHistoricos = listOf(
                ContaAberta(contaId, BigDecimal.ZERO),
                ContaBloqueadaEvento(contaId)
            )
        )

        val resultado = aggregate.processar(Depositar(BigDecimal.TEN, contaId))

        val commandRejeitado = assertIs<CommandRejeitado>(resultado)
        assertEquals(ContaBloqueada, commandRejeitado.erro)
    }

    @Test
    fun `saque em conta encerrada retorna conta encerrada`() {
        val contaId = UUID.randomUUID()
        val aggregate = ContaBancaria(
            eventosHistoricos = listOf(
                ContaAberta(contaId, BigDecimal.valueOf(100)),
                ContaEncerradaEvento(contaId)
            )
        )

        val resultado = aggregate.processar(Sacar(BigDecimal.TEN, contaId))

        val commandRejeitado = assertIs<CommandRejeitado>(resultado)
        assertEquals(ContaEncerrada, commandRejeitado.erro)
    }

    @Test
    fun `deposito com valor zero retorna valor invalido`() {
        val contaId = UUID.randomUUID()
        val aggregate = ContaBancaria(eventosHistoricos = listOf(ContaAberta(contaId, BigDecimal.ZERO)))

        val resultado = aggregate.processar(Depositar(BigDecimal.ZERO, contaId))

        val commandRejeitado = assertIs<CommandRejeitado>(resultado)
        assertEquals(ValorInvalido, commandRejeitado.erro)
    }

    @Test
    fun `deposito com valor negativo retorna valor invalido`() {
        val contaId = UUID.randomUUID()
        val aggregate = ContaBancaria(eventosHistoricos = listOf(ContaAberta(contaId, BigDecimal.ZERO)))

        val resultado = aggregate.processar(Depositar(BigDecimal.valueOf(-1), contaId))

        val commandRejeitado = assertIs<CommandRejeitado>(resultado)
        assertEquals(ValorInvalido, commandRejeitado.erro)
    }

    @Test
    fun `saque com valor zero retorna valor invalido`() {
        val contaId = UUID.randomUUID()
        val aggregate = ContaBancaria(eventosHistoricos = listOf(ContaAberta(contaId, BigDecimal.TEN)))

        val resultado = aggregate.processar(Sacar(BigDecimal.ZERO, contaId))

        val commandRejeitado = assertIs<CommandRejeitado>(resultado)
        assertEquals(ValorInvalido, commandRejeitado.erro)
    }

    @Test
    fun `saque com valor negativo retorna valor invalido`() {
        val contaId = UUID.randomUUID()
        val aggregate = ContaBancaria(eventosHistoricos = listOf(ContaAberta(contaId, BigDecimal.TEN)))

        val resultado = aggregate.processar(Sacar(BigDecimal.valueOf(-1), contaId))

        val commandRejeitado = assertIs<CommandRejeitado>(resultado)
        assertEquals(ValorInvalido, commandRejeitado.erro)
    }

    @Test
    fun `saque com saldo insuficiente retorna saldo insuficiente`() {
        val contaId = UUID.randomUUID()
        val aggregate = ContaBancaria(eventosHistoricos = listOf(ContaAberta(contaId, BigDecimal.TEN)))

        val resultado = aggregate.processar(Sacar(BigDecimal.valueOf(20), contaId))

        val commandRejeitado = assertIs<CommandRejeitado>(resultado)
        assertEquals(SaldoInsuficiente, commandRejeitado.erro)
    }

    @Test
    fun `saque em conta bloqueada retorna conta bloqueada`() {
        val contaId = UUID.randomUUID()
        val aggregate = ContaBancaria(
            eventosHistoricos = listOf(
                ContaAberta(contaId, BigDecimal.TEN),
                ContaBloqueadaEvento(contaId)
            )
        )

        val resultado = aggregate.processar(Sacar(BigDecimal.ONE, contaId))

        val commandRejeitado = assertIs<CommandRejeitado>(resultado)
        assertEquals(ContaBloqueada, commandRejeitado.erro)
    }

    @Test
    fun `deposito em conta encerrada retorna conta encerrada`() {
        val contaId = UUID.randomUUID()
        val aggregate = ContaBancaria(
            eventosHistoricos = listOf(
                ContaAberta(contaId, BigDecimal.ZERO),
                ContaEncerradaEvento(contaId)
            )
        )

        val resultado = aggregate.processar(Depositar(BigDecimal.TEN, contaId))

        val commandRejeitado = assertIs<CommandRejeitado>(resultado)
        assertEquals(ContaEncerrada, commandRejeitado.erro)
    }

    @Test
    fun `transferencia com valor zero retorna valor invalido`() {
        val contaOrigemId = UUID.randomUUID()
        val contaDestinoId = UUID.randomUUID()
        val aggregate = ContaBancaria(eventosHistoricos = listOf(ContaAberta(contaOrigemId, BigDecimal.TEN)))

        val resultado = aggregate.processar(Transferir(BigDecimal.ZERO, contaOrigemId, contaDestinoId))

        val commandRejeitado = assertIs<CommandRejeitado>(resultado)
        assertEquals(ValorInvalido, commandRejeitado.erro)
    }

    @Test
    fun `transferencia com valor negativo retorna valor invalido`() {
        val contaOrigemId = UUID.randomUUID()
        val contaDestinoId = UUID.randomUUID()
        val aggregate = ContaBancaria(eventosHistoricos = listOf(ContaAberta(contaOrigemId, BigDecimal.TEN)))

        val resultado = aggregate.processar(Transferir(BigDecimal.valueOf(-1), contaOrigemId, contaDestinoId))

        val commandRejeitado = assertIs<CommandRejeitado>(resultado)
        assertEquals(ValorInvalido, commandRejeitado.erro)
    }

    @Test
    fun `transferencia com saldo insuficiente retorna saldo insuficiente`() {
        val contaOrigemId = UUID.randomUUID()
        val contaDestinoId = UUID.randomUUID()
        val aggregate = ContaBancaria(eventosHistoricos = listOf(ContaAberta(contaOrigemId, BigDecimal.TEN)))

        val resultado = aggregate.processar(Transferir(BigDecimal.valueOf(20), contaOrigemId, contaDestinoId))

        val commandRejeitado = assertIs<CommandRejeitado>(resultado)
        assertEquals(SaldoInsuficiente, commandRejeitado.erro)
    }

    @Test
    fun `transferencia em conta bloqueada retorna conta bloqueada`() {
        val contaOrigemId = UUID.randomUUID()
        val contaDestinoId = UUID.randomUUID()
        val aggregate = ContaBancaria(
            eventosHistoricos = listOf(
                ContaAberta(contaOrigemId, BigDecimal.TEN),
                ContaBloqueadaEvento(contaOrigemId)
            )
        )

        val resultado = aggregate.processar(Transferir(BigDecimal.ONE, contaOrigemId, contaDestinoId))

        val commandRejeitado = assertIs<CommandRejeitado>(resultado)
        assertEquals(ContaBloqueada, commandRejeitado.erro)
    }

    @Test
    fun `transferencia em conta encerrada retorna conta encerrada`() {
        val contaOrigemId = UUID.randomUUID()
        val contaDestinoId = UUID.randomUUID()
        val aggregate = ContaBancaria(
            eventosHistoricos = listOf(
                ContaAberta(contaOrigemId, BigDecimal.TEN),
                ContaEncerradaEvento(contaOrigemId)
            )
        )

        val resultado = aggregate.processar(Transferir(BigDecimal.ONE, contaOrigemId, contaDestinoId))

        val commandRejeitado = assertIs<CommandRejeitado>(resultado)
        assertEquals(ContaEncerrada, commandRejeitado.erro)
    }

    @Test
    fun `bloquear conta ja bloqueada retorna conta bloqueada`() {
        val contaId = UUID.randomUUID()
        val aggregate = ContaBancaria(
            eventosHistoricos = listOf(
                ContaAberta(contaId, BigDecimal.ZERO),
                ContaBloqueadaEvento(contaId)
            )
        )

        val resultado = aggregate.processar(BloquearConta(contaId))

        val commandRejeitado = assertIs<CommandRejeitado>(resultado)
        assertEquals(ContaBloqueada, commandRejeitado.erro)
    }

    @Test
    fun `bloquear conta encerrada retorna conta encerrada`() {
        val contaId = UUID.randomUUID()
        val aggregate = ContaBancaria(
            eventosHistoricos = listOf(
                ContaAberta(contaId, BigDecimal.ZERO),
                ContaEncerradaEvento(contaId)
            )
        )

        val resultado = aggregate.processar(BloquearConta(contaId))

        val commandRejeitado = assertIs<CommandRejeitado>(resultado)
        assertEquals(ContaEncerrada, commandRejeitado.erro)
    }

    @Test
    fun `encerrar conta ja encerrada retorna conta encerrada`() {
        val contaId = UUID.randomUUID()
        val aggregate = ContaBancaria(
            eventosHistoricos = listOf(
                ContaAberta(contaId, BigDecimal.ZERO),
                ContaEncerradaEvento(contaId)
            )
        )

        val resultado = aggregate.processar(EncerrarConta(contaId))

        val commandRejeitado = assertIs<CommandRejeitado>(resultado)
        assertEquals(ContaEncerrada, commandRejeitado.erro)
    }

    @Test
    fun `encerrar conta bloqueada gera evento ContaEncerrada`() {
        val contaId = UUID.randomUUID()
        val aggregate = ContaBancaria(
            eventosHistoricos = listOf(
                ContaAberta(contaId, BigDecimal.ZERO),
                ContaBloqueadaEvento(contaId)
            )
        )

        val resultado = aggregate.processar(EncerrarConta(contaId))

        val commandAceito = assertIs<CommandAceito>(resultado)
        assertEquals(listOf(ContaEncerradaEvento(contaId)), commandAceito.eventos)
    }
}
