package br.com.funcional.banco.domain.commands

import java.math.BigDecimal
import java.util.*

sealed interface ContaCommands {
    val causationId: UUID
    val idConta: UUID
}

data class AbrirConta(
    override val causationId: UUID = UUID.randomUUID(),
    override val idConta: UUID,
    val saldoInicial: BigDecimal,
) : ContaCommands

data class Depositar(
    override val causationId: UUID = UUID.randomUUID(),
    val valor: BigDecimal,
    override val idConta: UUID
) :
    ContaCommands

data class Sacar(
    override val causationId: UUID = UUID.randomUUID(),
    val valor: BigDecimal,
    override val idConta: UUID
) : ContaCommands

data class Transferir(
    override val causationId: UUID = UUID.randomUUID(),
    val valor: BigDecimal,
    override val idConta: UUID,
    val idContaDestino: UUID
) : ContaCommands

data class BloquearConta(override val causationId: UUID = UUID.randomUUID(), override val idConta: UUID) :
    ContaCommands

data class EncerrarConta(override val causationId: UUID = UUID.randomUUID(), override val idConta: UUID) :
    ContaCommands


