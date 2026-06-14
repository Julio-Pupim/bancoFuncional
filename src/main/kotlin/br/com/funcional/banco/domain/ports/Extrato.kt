package br.com.funcional.banco.domain.ports

import br.com.funcional.banco.application.query.ExtratoReadModel
import java.math.BigDecimal
import java.util.*

interface Extrato {
    fun salvar(contaId: UUID, tipo: String, valor: BigDecimal = BigDecimal.ZERO)
    fun buscarExtrato(contaId: UUID): List<ExtratoReadModel>?
}