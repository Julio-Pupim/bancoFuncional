package br.com.funcional.banco.application.query

import java.math.BigDecimal
import java.util.*

data class ExtratoReadModel(
    val contaId: UUID,
    val tipo: String,
    val valor: BigDecimal
) {
}