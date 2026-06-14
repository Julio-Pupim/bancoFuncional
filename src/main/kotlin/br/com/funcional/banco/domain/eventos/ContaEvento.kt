package br.com.funcional.banco.domain.eventos

import java.math.BigDecimal
import java.util.*


sealed interface ContaEvento

data class ContaAberta(
    val id: UUID,
    val saldoInicial: BigDecimal
) : ContaEvento

data class DinheiroDepositado(val valor: BigDecimal, val idConta: UUID) : ContaEvento
data class DinheiroSacado(val valor: BigDecimal, val idConta: UUID) : ContaEvento
data class TransferenciaRealizada(val valor: BigDecimal, val idContaOrigem: UUID, val idContaDestino: UUID) :
    ContaEvento

data class ContaBloqueada(val idConta: UUID) : ContaEvento
data class ContaEncerrada(val idConta: UUID) : ContaEvento
