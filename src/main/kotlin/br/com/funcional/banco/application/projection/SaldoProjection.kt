package br.com.funcional.banco.application.projection

import br.com.funcional.banco.domain.eventos.*
import br.com.funcional.banco.domain.ports.ConsultarSaldo

class SaldoProjection(
    private val saldoRepository: ConsultarSaldo
) {

    fun handle(evento: ContaEvento) {
        when (evento) {
            is ContaAberta -> saldoRepository.salvar(evento.id, evento.saldoInicial)
            is DinheiroSacado -> saldoRepository.decrementarSaldo(evento.idConta, evento.valor)
            is DinheiroDepositado -> saldoRepository.incrementarSaldo(evento.idConta, evento.valor)
            is TransferenciaRealizada -> {
                saldoRepository.decrementarSaldo(evento.idContaOrigem, evento.valor)
                saldoRepository.incrementarSaldo(evento.idContaDestino, evento.valor)
            }

            else -> {}
        }

    }
}