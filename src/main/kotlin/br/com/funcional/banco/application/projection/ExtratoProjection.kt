package br.com.funcional.banco.application.projection

import br.com.funcional.banco.domain.eventos.ContaEvento
import br.com.funcional.banco.domain.eventos.DinheiroDepositado
import br.com.funcional.banco.domain.eventos.DinheiroSacado
import br.com.funcional.banco.domain.ports.Extrato

class ExtratoProjection(
    private val extratoRepository: Extrato,
) {

    fun handle(evento: ContaEvento) {
        when (evento) {
            is DinheiroDepositado -> extratoRepository.salvar(evento.idConta, "DEPOSITO", evento.valor)
            is DinheiroSacado -> extratoRepository.salvar(evento.idConta, "SAQUE", evento.valor)
            else -> {}
        }
    }
}