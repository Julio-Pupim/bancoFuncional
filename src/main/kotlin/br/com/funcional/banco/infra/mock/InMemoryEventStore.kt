package br.com.funcional.banco.infra.mock

import br.com.funcional.banco.domain.eventos.ContaEvento
import br.com.funcional.banco.domain.exception.ConflitoVersaoException
import br.com.funcional.banco.domain.ports.EventStore
import br.com.funcional.banco.domain.ports.MetadadosEvento
import br.com.funcional.banco.infra.models.EventoPersistido
import tools.jackson.databind.ObjectMapper
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class InMemoryEventStore(
    private val objectMapper: ObjectMapper
) : EventStore {

    // Simula nossa tabela do banco de dados
    private val store = ConcurrentHashMap<UUID, MutableList<EventoPersistido>>()

    override fun append(id: UUID, eventos: List<ContaEvento>, versaoAtual: Long, metadadosEvento: MetadadosEvento) {
        val stream = store.computeIfAbsent(id) { mutableListOf() }

        // Simula o Optimistic Locking do banco
        if (stream.size.toLong() != versaoAtual) {
            throw ConflitoVersaoException("Conflito de concorrência: versão esperada $versaoAtual, versão atual ${stream.size}")
        }

        var proximaVersao = versaoAtual + 1

        val eventosPersistidos = eventos.map { evento ->
            val payloadJson = objectMapper.writeValueAsString(evento)
            val persistido = EventoPersistido(
                eventId = UUID.randomUUID(),
                aggregateId = id,
                aggregateType = "ContaBancaria",
                aggregateVersion = proximaVersao,
                eventType = evento.javaClass.simpleName,
                schemaVersion = 1,
                occurredAt = LocalDateTime.now(),
                payload = payloadJson,
                correlationId = metadadosEvento.correlationId,
                causationId = metadadosEvento.causationId,
            )
            proximaVersao++
            persistido
        }

        stream.addAll(eventosPersistidos)
    }

    override fun load(aggregateId: UUID): List<EventoPersistido> {
        return store[aggregateId]?.toList() ?: emptyList()
    }
}