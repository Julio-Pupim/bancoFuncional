package br.com.funcional.banco.domain.ports

import br.com.funcional.banco.domain.eventos.ContaEvento
import br.com.funcional.banco.infra.models.EventoPersistido
import java.util.*

interface EventStore {
    fun append(id: UUID, eventos: List<ContaEvento>, versaoAtual: Long)
    fun load(aggregateId: UUID): List<EventoPersistido>
}