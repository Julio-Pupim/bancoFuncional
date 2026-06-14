package br.com.funcional.banco.domain.ports

import java.math.BigDecimal
import java.util.*

interface ConsultarSaldo {

    fun salvar(contaId: UUID, saldo: BigDecimal = BigDecimal.ZERO)
    fun incrementarSaldo(contaId: UUID, valor: BigDecimal)
    fun decrementarSaldo(contaId: UUID, valor: BigDecimal)
    fun buscarSaldo(contaId: UUID): BigDecimal
}