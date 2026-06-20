package br.com.funcional.banco.application.repository

import br.com.funcional.banco.domain.eventos.ContaEvento
import br.com.funcional.banco.domain.exception.ConflitoVersaoException
import br.com.funcional.banco.domain.exception.EventoInconsistenteException
import br.com.funcional.banco.domain.ports.EventStore
import br.com.funcional.banco.domain.ports.MetadadosEvento
import br.com.funcional.banco.infra.models.EventoPersistido
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.time.LocalDateTime
import java.util.*

@Repository
class EventStoreRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val objectMapper: ObjectMapper
) : EventStore {
    @Transactional
    override fun append(id: UUID, eventos: List<ContaEvento>, versaoAtual: Long, metadadosEvento: MetadadosEvento) {

        if (eventos.isEmpty())
            return
        if (getMaxAggregateVersion(id) != versaoAtual) {
            throw ConflitoVersaoException("versao do banco diferente")
        }
        var proximaVersao = versaoAtual + 1L
        val persistirLinhas = eventos.map { evento ->
            val linha = EventoPersistido(
                UUID.randomUUID(),
                id,
                "ContaBancaria",
                proximaVersao,
                evento.javaClass.simpleName,
                1,
                LocalDateTime.now(),
                objectMapper.writeValueAsString(evento),
                metadadosEvento.correlationId,
                metadadosEvento.causationId,
            )
            proximaVersao++
            linha
        }
        salvarNoBanco(persistirLinhas)
    }

    private fun salvarNoBanco(linhas: List<EventoPersistido>) {
        val sql = """
            insert into event_store(event_id, aggregate_id, aggregate_type, aggregate_version, event_type, schema_version, occurred_at, payload, correlation_id, causation_id)
            values (:eventId, :aggregateId, :aggregateType, :aggregateVersion, :eventType, :schemaVersion, :occurredAt, cast(:payload as jsonb), :correlationId, :causationId)
        """.trimIndent()
        val batchArgs = SqlParameterSourceUtils.createBatch(linhas)
        jdbcTemplate.batchUpdate(sql, batchArgs)
    }

    override fun load(aggregateId: UUID): List<EventoPersistido> {
        val sql = """
            select
                event_id,
                aggregate_id,
                aggregate_type,
                aggregate_version,
                event_type,
                schema_version,
                occurred_at,
                payload::text as payload,
                correlation_id,
                causation_id
            from event_store
            where aggregate_id = :aggregate_id
            order by aggregate_version
        """.trimIndent()

        val query = jdbcTemplate.query(
            sql,
            mapOf("aggregate_id" to aggregateId)
        ) { rs, _ ->
            EventoPersistido(
                rs.getObject("event_id", UUID::class.java),
                rs.getObject("aggregate_id", UUID::class.java),
                rs.getString("aggregate_type"),
                rs.getLong("aggregate_version"),
                rs.getString("event_type"),
                rs.getInt("schema_version"),
                rs.getObject("occurred_at", LocalDateTime::class.java),
                rs.getString("payload"),
                rs.getObject("correlation_id", UUID::class.java),
                rs.getObject("causation_id", UUID::class.java),
            )
        }
        validarIntegridade(query)

        return query
    }

    private fun getMaxAggregateVersion(aggregateId: UUID): Long {
        val sql = """
            select max(aggregate_version) from event_store where aggregate_id = :aggregate_id
        """.trimIndent()
        val params = mapOf("aggregate_id" to aggregateId)
        return jdbcTemplate.queryForObject(sql, params, Long::class.java) ?: 0
    }

    private fun validarIntegridade(query: List<EventoPersistido>) {
        val versao = query.maxOfOrNull { it.aggregateVersion } ?: return

        if (query.size.toLong() != versao) {
            throw EventoInconsistenteException("lacuna detectada: esperado ${query.size} eventos, versão máxima é $versao")
        }
    }

}