package br.com.funcional.banco

import br.com.funcional.banco.domain.eventos.*
import br.com.funcional.banco.infra.mapper.EventoMapper
import br.com.funcional.banco.infra.models.EventoPersistido
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertIs

class EventoMapperTest {

    private val mapper = EventoMapper()

    @Test
    fun `deve mapear evento persistido para ContaAberta`() {
        val aggregateId = UUID.randomUUID()
        val eventoPersistido = EventoPersistido(
            eventId = UUID.randomUUID(),
            aggregateId = aggregateId,
            aggregateType = "ContaBancaria",
            aggregateVersion = 1,
            eventType = "ContaAberta",
            schemaVersion = 1,
            occurredAt = LocalDateTime.now(),
            payload = """{"id":"$aggregateId","saldoInicial":100.00}""",
            correlationId = UUID.randomUUID(),
            causationId = UUID.randomUUID(),
        )

        val evento = mapper.mapEventoPersistidoToContaEvento(eventoPersistido)

        val contaAberta = assertIs<ContaAberta>(evento)
        assertEquals(aggregateId, contaAberta.id)
        assertEquals(0, BigDecimal("100.00").compareTo(contaAberta.saldoInicial))
    }

    @Test
    fun `deve mapear evento persistido para DinheiroDepositado`() {
        val aggregateId = UUID.randomUUID()
        val eventoPersistido = EventoPersistido(
            eventId = UUID.randomUUID(),
            aggregateId = aggregateId,
            aggregateType = "ContaBancaria",
            aggregateVersion = 1,
            eventType = "DinheiroDepositado",
            schemaVersion = 1,
            occurredAt = LocalDateTime.now(),
            payload = """{"idConta":"$aggregateId","valor":100.00}""",
            correlationId = UUID.randomUUID(),
            causationId = UUID.randomUUID(),
        )
        val evento = mapper.mapEventoPersistidoToContaEvento(eventoPersistido)

        val dinheiroDepositado = assertIs<DinheiroDepositado>(evento)
        assertEquals(aggregateId, dinheiroDepositado.idConta)
        assertEquals(0, BigDecimal("100.00").compareTo(dinheiroDepositado.valor))
    }

    @Test
    fun `deve mapear evento persistido para DinheiroSacado`() {
        val aggregateId = UUID.randomUUID()
        val eventoPersistido = EventoPersistido(
            eventId = UUID.randomUUID(),
            aggregateId = aggregateId,
            aggregateType = "DinheiroSacado",
            aggregateVersion = 1,
            eventType = "DinheiroSacado",
            schemaVersion = 1,
            occurredAt = LocalDateTime.now(),
            payload = """{"idConta":"$aggregateId","valor":100.00}""",
            correlationId = UUID.randomUUID(),
            causationId = UUID.randomUUID(),
        )
        val evento = mapper.mapEventoPersistidoToContaEvento(eventoPersistido)
        val dinheiroSacado = assertIs<DinheiroSacado>(evento)
        assertEquals(aggregateId, dinheiroSacado.idConta)
        assertEquals(0, BigDecimal(100.00).compareTo(dinheiroSacado.valor))
    }

    @Test
    fun `deve mapear evento para TransferenciaRealizada`() {
        val aggregateId = UUID.randomUUID()
        val outroId = UUID.randomUUID()
        val eventoPersistido = EventoPersistido(
            eventId = UUID.randomUUID(),
            aggregateId = aggregateId,
            aggregateType = "ContaBancaria",
            aggregateVersion = 1,
            eventType = "TransferenciaRealizada",
            schemaVersion = 1,
            occurredAt = LocalDateTime.now(),
            payload = """{"valor":100.00, "idContaOrigem":"$aggregateId", "idContaDestino":"$outroId"}""",
            correlationId = UUID.randomUUID(),
            causationId = UUID.randomUUID(),
        )
        val evento = mapper.mapEventoPersistidoToContaEvento(eventoPersistido)
        val transferenciaRealizada = assertIs<TransferenciaRealizada>(evento)
        assertEquals(aggregateId, transferenciaRealizada.idContaOrigem)
        assertEquals(outroId, transferenciaRealizada.idContaDestino)
        assertEquals(0, BigDecimal(100.00).compareTo(transferenciaRealizada.valor))
    }

    @Test
    fun `deve mapear evento para Conta Bloqueada`() {
        val aggregateId = UUID.randomUUID()
        val eventoPersistido = EventoPersistido(
            eventId = UUID.randomUUID(),
            aggregateId = aggregateId,
            aggregateType = "ContaBancaria",
            aggregateVersion = 1,
            eventType = "ContaBloqueada",
            schemaVersion = 1,
            occurredAt = LocalDateTime.now(),
            payload = """{"idConta": "$aggregateId"}""",
            correlationId = UUID.randomUUID(),
            causationId = UUID.randomUUID(),
        )
        val evento = mapper.mapEventoPersistidoToContaEvento(eventoPersistido)
        val contaBloqueada = assertIs<ContaBloqueada>(evento)
        assertEquals(aggregateId, contaBloqueada.idConta)
    }

    @Test
    fun `deve mapear evento para ContaEncerrada`() {
        val aggregateId = UUID.randomUUID()
        val eventoPersistido = EventoPersistido(
            eventId = UUID.randomUUID(),
            aggregateId = aggregateId,
            aggregateType = "ContaBancaria",
            aggregateVersion = 1,
            eventType = "ContaEncerrada",
            schemaVersion = 1,
            occurredAt = LocalDateTime.now(),
            payload = """{"idConta":"$aggregateId"}""",
            correlationId = UUID.randomUUID(),
            causationId = UUID.randomUUID(),
        )
        val evento = mapper.mapEventoPersistidoToContaEvento(eventoPersistido)
        val contaEncerrada = assertIs<ContaEncerrada>(evento)
        assertEquals(aggregateId, contaEncerrada.idConta)
    }
}
