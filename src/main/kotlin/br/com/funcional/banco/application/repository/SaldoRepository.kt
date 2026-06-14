package br.com.funcional.banco.application.repository

import br.com.funcional.banco.domain.ports.ConsultarSaldo
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.util.*

@Repository
class SaldoRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate
) : ConsultarSaldo {

    override fun salvar(contaId: UUID, saldo: BigDecimal) {
        val params = mapOf("contaId" to contaId, "saldo" to saldo)
        jdbcTemplate.update("""insert into saldo_conta(contaId, saldo) values (:contaId, :saldo);""", params)
    }

    override fun incrementarSaldo(contaId: UUID, saldo: BigDecimal) {
        val params = mapOf("contaId" to contaId, "saldo" to saldo)
        jdbcTemplate.update("""update saldo_conta set saldo = saldo + :saldo where contaId = :contaId;""", params)
    }

    override fun decrementarSaldo(contaId: UUID, saldo: BigDecimal) {
        val params = mapOf("contaId" to contaId, "saldo" to saldo)
        jdbcTemplate.update("""update saldo_conta set saldo = saldo - :saldo where contaId = :contaId;""", params)
    }

    override fun buscarSaldo(contaId: UUID): BigDecimal {
        return jdbcTemplate.queryForObject(
            """select saldo from saldo_conta where contaId = :contaId""",
            mapOf("contaId" to contaId), BigDecimal::class.java
        ) ?: throw IllegalArgumentException(
            "Conta não encontrada"
        )
    }

}