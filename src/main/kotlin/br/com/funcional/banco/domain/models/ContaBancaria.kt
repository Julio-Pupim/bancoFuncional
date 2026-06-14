package br.com.funcional.banco.domain.models

import br.com.funcional.banco.domain.commands.*
import br.com.funcional.banco.domain.commands.ContaBloqueada
import br.com.funcional.banco.domain.commands.ContaEncerrada
import br.com.funcional.banco.domain.eventos.*
import java.math.BigDecimal
import java.util.*
import br.com.funcional.banco.domain.eventos.ContaBloqueada as ContaBloqueadaEvento
import br.com.funcional.banco.domain.eventos.ContaEncerrada as ContaEncerradaEvento

class ContaBancaria(
    private val eventosHistoricos: List<ContaEvento> = emptyList()
) {
    val versaoAtual: Long
        get() = eventosHistoricos.size.toLong()

    fun processar(command: ContaCommands): ResultadoCommand =
        when (command) {
            is AbrirConta -> abrirConta(command)
            is Depositar -> depositarDinheiro(command)
            is Sacar -> sacarDinheiro(command)
            is Transferir -> transferirDinheiro(command)
            is BloquearConta -> bloquearConta(command)
            is EncerrarConta -> encerrarConta(command)
        }

    private fun encerrarConta(command: EncerrarConta): ResultadoCommand {
        validarContaAberta(command.idConta)?.let { return it }

        if (estadoAtualDaConta(command.idConta)?.encerrada == true) {
            return CommandRejeitado(ContaEncerrada)
        }

        return CommandAceito(listOf(ContaEncerradaEvento(command.idConta)))
    }

    private fun bloquearConta(command: BloquearConta): ResultadoCommand {
        validarContaAberta(command.idConta)?.let { return it }

        val estado = estadoAtualDaConta(command.idConta)
        if (estado?.encerrada == true) {
            return CommandRejeitado(ContaEncerrada)
        }
        if (estado?.bloqueada == true) {
            return CommandRejeitado(ContaBloqueada)
        }

        return CommandAceito(listOf(ContaBloqueadaEvento(command.idConta)))
    }

    private fun transferirDinheiro(command: Transferir): ResultadoCommand {
        validarContaAberta(command.idConta)?.let { return it }
        validarContaOperacional(command.idConta)?.let { return it }

        if (command.idConta == command.idContaDestino) {
            return CommandRejeitado(ContaDestinoInvalida)
        }
        if (valorInvalido(command.valor)) {
            return CommandRejeitado(ValorInvalido)
        }

        val estado = estadoAtualDaConta(command.idConta) ?: return CommandRejeitado(ContaNaoAberta)
        if (command.valor > estado.saldo) {
            return CommandRejeitado(SaldoInsuficiente)
        }

        return CommandAceito(
            eventos = listOf(
                TransferenciaRealizada(
                    command.valor,
                    command.idConta,
                    command.idContaDestino
                )
            )
        )
    }

    private fun sacarDinheiro(command: Sacar): ResultadoCommand {
        validarContaAberta(command.idConta)?.let { return it }
        validarContaOperacional(command.idConta)?.let { return it }

        if (valorInvalido(command.valor)) {
            return CommandRejeitado(ValorInvalido)
        }

        val estado = estadoAtualDaConta(command.idConta) ?: return CommandRejeitado(ContaNaoAberta)
        if (command.valor > estado.saldo) {
            return CommandRejeitado(SaldoInsuficiente)
        }

        return CommandAceito(eventos = listOf(DinheiroSacado(command.valor, command.idConta)))
    }

    private fun depositarDinheiro(command: Depositar): ResultadoCommand {
        validarContaAberta(command.idConta)?.let { return it }
        validarContaOperacional(command.idConta)?.let { return it }

        if (valorInvalido(command.valor)) {
            return CommandRejeitado(ValorInvalido)
        }

        return CommandAceito(eventos = listOf(DinheiroDepositado(command.valor, command.idConta)))
    }

    private fun abrirConta(command: AbrirConta): ResultadoCommand {
        val contaJaFoiAberta = eventosHistoricos.any { it is ContaAberta }

        if (contaJaFoiAberta) {
            return CommandRejeitado(ContaJaAberta)
        }

        // O aggregate nao muda estado diretamente: ele decide qual fato novo nasceu.
        return CommandAceito(
            eventos = listOf(
                ContaAberta(
                    id = command.idConta,
                    saldoInicial = command.saldoInicial
                )
            )
        )
    }

    private fun validarContaAberta(idConta: UUID): CommandRejeitado? =
        if (contaExisteNoHistorico(idConta)) null else CommandRejeitado(ContaNaoAberta)

    private fun validarContaOperacional(idConta: UUID): CommandRejeitado? {
        val estado = estadoAtualDaConta(idConta) ?: return CommandRejeitado(ContaNaoAberta)

        return when {
            estado.encerrada -> CommandRejeitado(ContaEncerrada)
            estado.bloqueada -> CommandRejeitado(ContaBloqueada)
            else -> null
        }
    }

    private fun contaExisteNoHistorico(idConta: UUID): Boolean =
        eventosHistoricos
            .filterIsInstance<ContaAberta>()
            .any { contaAberta -> contaAberta.id == idConta }

    private fun estadoAtualDaConta(idConta: UUID): ContaEstado? =
        estadoAtual()?.takeIf { estado -> estado.id == idConta }

    private fun valorInvalido(valor: BigDecimal): Boolean =
        valor <= BigDecimal.ZERO

    fun estadoAtual(): ContaEstado? =
        reconstruirEstado(eventosHistoricos)

    companion object {
        fun reconstruirEstado(eventos: List<ContaEvento>): ContaEstado? =
            eventos.fold(null as ContaEstado?) { estado, evento ->
                // O estado e derivado do historico; antes de ContaAberta, a conta nao existe.
                when (evento) {
                    is ContaAberta -> ContaEstado(
                        id = evento.id,
                        saldo = evento.saldoInicial,
                        ativa = true,
                        bloqueada = false,
                        encerrada = false
                    )

                    is DinheiroDepositado ->
                        estado?.copy(
                            saldo = estado.saldo.plus(evento.valor)
                        )

                    is DinheiroSacado ->
                        estado?.copy(
                            saldo = estado.saldo.subtract(evento.valor)
                        )

                    is TransferenciaRealizada ->
                        estado?.copy(saldo = estado.saldo.subtract(evento.valor))

                    is ContaBloqueadaEvento -> estado?.copy(bloqueada = true)

                    is ContaEncerradaEvento -> estado?.copy(
                        ativa = false,
                        encerrada = true
                    )
                }
            }
    }
}
