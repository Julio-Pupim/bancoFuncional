package br.com.funcional.banco.repository

import br.com.funcional.banco.application.repository.ContaBancariaRepository
import br.com.funcional.banco.domain.eventos.ContaAberta
import br.com.funcional.banco.domain.eventos.DinheiroDepositado
import br.com.funcional.banco.infra.mapper.EventoMapper
import br.com.funcional.banco.infra.mock.InMemoryEventStore
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.math.BigDecimal
import java.util.*

class ContaBancariaRepositoryTest {

    private val objectMapper = jacksonObjectMapper()
    private val eventStore = InMemoryEventStore(objectMapper)
    private val eventoMapper = EventoMapper(objectMapper)

    // O System Under Test (SUT)
    private val repository = ContaBancariaRepository(eventStore, eventoMapper)

    @Test
    fun `deve reidratar uma ContaBancaria com o historico de eventos correto`() {
        // Arrange
        val id = UUID.randomUUID()
        val evento1 = ContaAberta(id, BigDecimal("100.00"))
        val evento2 = DinheiroDepositado(BigDecimal("50.00"), id)

        // Populamos o mock via porta
        eventStore.append(id, listOf(evento1, evento2), 0L)

        // Act
        val contaReidratada = repository.buscarPorId(id)

        // Assert
        assertNotNull(contaReidratada)
        assertEquals(2L, contaReidratada!!.versaoAtual)

        val estado = contaReidratada.estadoAtual()!!
        assertEquals(BigDecimal("150.00"), estado.saldo)
    }

    @Test
    fun `deve retornar null quando o stream de eventos nao existir`() {
        // Act
        val conta = repository.buscarPorId(UUID.randomUUID())

        // Assert
        assertNull(conta)
    }
}