package br.com.funcional.banco.domain.models

import java.math.BigDecimal
import java.util.UUID

data class ContaEstado(
    val id: UUID,
    val saldo: BigDecimal,
    val ativa: Boolean,
    val bloqueada: Boolean,
    val encerrada: Boolean
)
