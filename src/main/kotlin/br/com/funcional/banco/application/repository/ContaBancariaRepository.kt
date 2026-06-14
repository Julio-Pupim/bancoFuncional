package br.com.funcional.banco.application.repository

import br.com.funcional.banco.domain.eventos.ContaEvento
import br.com.funcional.banco.domain.models.ContaBancaria
import br.com.funcional.banco.domain.ports.EventStore
import br.com.funcional.banco.infra.mapper.EventoMapper
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class ContaBancariaRepository(private val eventStore: EventStore, val eventoMapper: EventoMapper) {

    fun buscarPorId(id: UUID): ContaBancaria? {
        val eventosPersistidos = eventStore.load(id)

        if (eventosPersistidos.isEmpty()) {
            return null
        }
        val eventosHistoricos =
            eventosPersistidos.map { evento -> eventoMapper.mapEventoPersistidoToContaEvento(evento) }

        return ContaBancaria(eventosHistoricos)
    }

    fun salvar(id: UUID, novosEventos: List<ContaEvento>, versaoAtual: Long) {
        eventStore.append(id, novosEventos, versaoAtual)
    }
}