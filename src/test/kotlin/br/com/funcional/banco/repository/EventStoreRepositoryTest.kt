package br.com.funcional.banco.repository

import br.com.funcional.banco.IntegrationTestBase
import br.com.funcional.banco.application.repository.EventStoreRepository
import br.com.funcional.banco.domain.eventos.ContaAberta
import br.com.funcional.banco.domain.eventos.ContaBloqueada
import br.com.funcional.banco.domain.eventos.DinheiroDepositado
import br.com.funcional.banco.domain.eventos.DinheiroSacado
import br.com.funcional.banco.infra.models.EventoPersistido
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.math.BigDecimal
import java.util.*
import kotlin.test.assertEquals

class EventStoreRepositoryTest : IntegrationTestBase() {

    @Autowired
    lateinit var repository: EventStoreRepository

    @Autowired
    lateinit var jdbcTemplate: NamedParameterJdbcTemplate

    @Test
    fun `deve persistir evento`() {

        val aggregateId = UUID.randomUUID()

        val evento = ContaAberta(
            aggregateId,
            BigDecimal("100.00")
        )

        repository.append(
            aggregateId,
            listOf(evento),
            0
        )

        val quantidade = jdbcTemplate.jdbcTemplate.queryForObject(
            """
            select count(*)
            from event_store
            where aggregate_id = ?
            """.trimIndent(),
            Long::class.java,
            aggregateId
        )
        val eventosPersistidos = repository.load(aggregateId)

        assertEquals(1L, quantidade)
        assertEquals(1L, eventosPersistidos.first().aggregateVersion)
    }

    @Test
    fun `versao errada nao persiste`() {
        val aggregateId = UUID.randomUUID()
        val evento = ContaAberta(aggregateId, BigDecimal("100.00"))
        assertThrows<Exception>("versao do banco diferente") {
            repository.append(
                aggregateId,
                listOf(evento),
                5
            )
        }
    }

    @Test
    fun `persistir multiplos eventos`() {
        val aggregateId = UUID.randomUUID()
        val evento = ContaAberta(aggregateId, BigDecimal("100.00"))
        val evento2 = DinheiroDepositado(BigDecimal("100.00"), aggregateId)
        val evento3 = DinheiroSacado(BigDecimal("100.00"), aggregateId)
        repository.append(
            aggregateId,
            listOf(evento, evento2, evento3),
            0
        )
        val versoes = repository.load(aggregateId)

        assertEquals(listOf(1L, 2L, 3L), versoes.map { it.aggregateVersion })
        assertEquals(listOf("ContaAberta", "DinheiroDepositado", "DinheiroSacado"), versoes.map { it.eventType })
    }

    @Test
    fun `deve testar atomicidade do append`() {
        val aggregateId = UUID.randomUUID()
        val evento1 = ContaAberta(aggregateId, BigDecimal("100.00"))
        val evento2 = DinheiroDepositado(BigDecimal("100.00"), aggregateId)
        val evento3 = ContaBloqueada(aggregateId) // Este evento fará o lote falhar

        // 1. Criamos uma constraint temporária direto no banco que proíbe o evento "ContaBloqueada".
        // Isso simulará uma falha no meio do batch update.
        jdbcTemplate.jdbcTemplate.execute(
            "ALTER TABLE event_store ADD CONSTRAINT chk_simula_falha_lote CHECK (event_type != 'ContaBloqueada')"
        )

        try {
            // 2. Executamos o append. O banco vai rejeitar o evento3 e falhar a transação.
            assertThrows<Exception>("Deve lançar exceção do banco de dados") {
                repository.append(aggregateId, listOf(evento1, evento2, evento3), 0)
            }

            // 3. Validamos a atomicidade: como o lote falhou, o evento1 e evento2 NÃO devem ter sido salvos.
            val eventosPersistidos = repository.load(aggregateId)
            assertEquals(
                emptyList<EventoPersistido>(),
                eventosPersistidos,
                "Nenhum evento deve ser persistido devido ao rollback da transação"
            )

        } finally {
            // 4. Limpamos a constraint para não quebrar os outros testes da suíte
            jdbcTemplate.jdbcTemplate.execute(
                "ALTER TABLE event_store DROP CONSTRAINT chk_simula_falha_lote"
            )
        }
    }

    @Test
    fun `load deve retornar lista vazia quando stream nao existe`() {
        val eventosPersistidos = repository.load(UUID.randomUUID())

        assertEquals(emptyList(), eventosPersistidos)
    }

    @Test
    fun `load deve retornar apenas eventos do aggregate informado em ordem`() {
        val aggregateId = UUID.randomUUID()
        val outroAggregateId = UUID.randomUUID()

        repository.append(
            aggregateId,
            listOf(
                ContaAberta(aggregateId, BigDecimal("100.00")),
                DinheiroDepositado(BigDecimal("50.00"), aggregateId),
                DinheiroSacado(BigDecimal("20.00"), aggregateId)
            ),
            0
        )
        repository.append(
            outroAggregateId,
            listOf(ContaAberta(outroAggregateId, BigDecimal("200.00"))),
            0
        )

        val eventosPersistidos = repository.load(aggregateId)

        assertEquals(listOf(1L, 2L, 3L), eventosPersistidos.map { it.aggregateVersion })
        assertEquals(listOf(aggregateId, aggregateId, aggregateId), eventosPersistidos.map { it.aggregateId })
        assertEquals(
            listOf("ContaAberta", "DinheiroDepositado", "DinheiroSacado"),
            eventosPersistidos.map { it.eventType }
        )
    }
}
