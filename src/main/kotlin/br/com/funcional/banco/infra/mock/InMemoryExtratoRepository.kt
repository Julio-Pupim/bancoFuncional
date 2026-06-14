package br.com.funcional.banco.infra.mock

import br.com.funcional.banco.application.query.ExtratoReadModel
import br.com.funcional.banco.domain.ports.Extrato
import java.math.BigDecimal
import java.util.*

class InMemoryExtratoRepository() : Extrato {
    private val extratos =
        mutableMapOf<UUID, MutableList<ExtratoReadModel>>()

    override fun salvar(contaId: UUID, tipo: String, valor: BigDecimal) {
        val lista = extratos.getOrPut(contaId) {
            mutableListOf()
        }

        lista.add(
            ExtratoReadModel(
                contaId,
                tipo,
                valor
            )
        )
    }

    override fun buscarExtrato(contaId: UUID): List<ExtratoReadModel> {
        return extratos[contaId]
            ?.toList()
            ?: emptyList()
    }

}