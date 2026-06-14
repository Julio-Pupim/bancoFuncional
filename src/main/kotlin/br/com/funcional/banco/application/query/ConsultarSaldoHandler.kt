package br.com.funcional.banco.application.query

import br.com.funcional.banco.domain.ports.ConsultarSaldo
import java.math.BigDecimal

class ConsultarSaldoHandler(
    private val repository: ConsultarSaldo
) {

    fun handle(
        query: ConsultaSaldo
    ): BigDecimal {

        return repository.buscarSaldo(
            query.contaId
        )
    }
}