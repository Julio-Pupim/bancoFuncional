package br.com.funcional.banco.infra.mock

import br.com.funcional.banco.domain.ports.ConsultarSaldo
import java.math.BigDecimal
import java.util.*

class InMemorySaldoRepository(

) : ConsultarSaldo {
    private val saldos =
        mutableMapOf<UUID, BigDecimal>()

    override fun salvar(contaId: UUID, saldo: BigDecimal) {
        saldos[contaId] = saldo
    }

    override fun incrementarSaldo(contaId: UUID, valor: BigDecimal) {
        val saldoAtual =
            saldos[contaId] ?: BigDecimal.ZERO

        saldos[contaId] =
            saldoAtual + valor
    }

    override fun decrementarSaldo(contaId: UUID, valor: BigDecimal) {
        val saldoAtual =
            saldos[contaId] ?: BigDecimal.ZERO

        saldos[contaId] =
            saldoAtual - valor
    }

    override fun buscarSaldo(contaId: UUID): BigDecimal =
        saldos[contaId] ?: throw IllegalArgumentException(
            "Conta não encontrada"
        )

}