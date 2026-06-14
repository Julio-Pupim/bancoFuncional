package br.com.funcional.banco.application.repository

import br.com.funcional.banco.application.query.ExtratoReadModel
import br.com.funcional.banco.domain.ports.Extrato
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.util.*

@Repository

class ExtratoRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate
) : Extrato {
    override fun salvar(contaId: UUID, tipo: String, valor: BigDecimal) {
        val params = mapOf("contaId" to contaId, "valor" to valor, "tipo" to tipo)
        jdbcTemplate.update(
            """insert into extrato_conta(contaId,tipo, valor) values (:contaId, :tipo, :valor);""",
            params
        )
    }

    override fun buscarExtrato(contaId: UUID): List<ExtratoReadModel>? {
        return jdbcTemplate.query(
            """
        select conta_id, tipo, valor
        from extrato_conta
        where conta_id = :contaId
        """,
            mapOf("contaId" to contaId)
        ) { rs, _ ->

            ExtratoReadModel(
                contaId = rs.getObject("conta_id", UUID::class.java),
                tipo = rs.getString("tipo"),
                valor = rs.getBigDecimal("valor")
            )
        }

    }
}