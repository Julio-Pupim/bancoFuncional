package br.com.funcional.banco.domain.commands

import java.math.BigDecimal
import java.util.*

sealed interface ContaCommands {
    val idConta: UUID
}

data class AbrirConta(
    override val idConta: UUID,
    val saldoInicial: BigDecimal
) : ContaCommands

data class Depositar(val valor: BigDecimal, override val idConta: UUID) : ContaCommands
data class Sacar(val valor: BigDecimal, override val idConta: UUID) : ContaCommands
data class Transferir(val valor: BigDecimal, override val idConta: UUID, val idContaDestino: UUID) : ContaCommands
data class BloquearConta(override val idConta: UUID) : ContaCommands
data class EncerrarConta(override val idConta: UUID) : ContaCommands


