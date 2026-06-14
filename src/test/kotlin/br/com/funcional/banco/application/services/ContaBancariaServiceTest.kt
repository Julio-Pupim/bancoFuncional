package br.com.funcional.banco.application.services

import br.com.funcional.banco.application.repository.ContaBancariaRepository
import br.com.funcional.banco.domain.commands.*
import br.com.funcional.banco.domain.eventos.ContaAberta
import br.com.funcional.banco.infra.mapper.EventoMapper
import br.com.funcional.banco.infra.mock.InMemoryEventStore
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal
import java.util.*
import kotlin.test.*

class ContaBancariaServiceTest {

    private val eventStore: InMemoryEventStore = InMemoryEventStore(ObjectMapper())
    private val eventoMapper: EventoMapper = EventoMapper()
    private val repository: ContaBancariaRepository = ContaBancariaRepository(eventStore, eventoMapper)
    private val contaBancariaService: ContaBancariaService = ContaBancariaService(repository)

    @Test
    fun `deve abrir conta`() {
        val id = UUID.randomUUID()
        val comando = AbrirConta(id, BigDecimal.ZERO)
        val novoComando = contaBancariaService.novoComando(comando)
        val eventos = (novoComando as CommandAceito).eventos
        assertIs<CommandAceito>(novoComando)
        assertEquals(1, eventos.size)
        assertTrue { eventos.first() is ContaAberta }
        val eventosNoBanco = eventStore.load(id)
        assertEquals(1, eventosNoBanco.size)
        assertEquals("ContaAberta", eventosNoBanco.first().eventType)
    }

    @Test
    fun `deve abrir conta, depositar, sacar e encerrar conta`() {

        val idConta = UUID.randomUUID()
        val comandoAbrirConta = AbrirConta(idConta, BigDecimal.ZERO)
        val comandoDepositar = Depositar(BigDecimal.TEN, idConta)
        val comandoSacar = Sacar(BigDecimal.TEN, idConta)
        val comandoEncerrar = EncerrarConta(idConta)
        val comandoAbrirContaProcessado = contaBancariaService.novoComando(comandoAbrirConta)
        val comandoDepositarProcessado = contaBancariaService.novoComando(comandoDepositar)
        val comandoSacarProcessado = contaBancariaService.novoComando(comandoSacar)
        val comandoEncerrarProcessado = contaBancariaService.novoComando(comandoEncerrar)

        assertIs<CommandAceito>(comandoAbrirContaProcessado)
        assertIs<CommandAceito>(comandoDepositarProcessado)
        assertIs<CommandAceito>(comandoSacarProcessado)
        assertIs<CommandAceito>(comandoEncerrarProcessado)

        val eventosPersistidos = eventStore.load(idConta)

        assertEquals(4, eventosPersistidos.size)
        assertEquals("ContaAberta", eventosPersistidos.first().eventType)
        assertEquals("DinheiroDepositado", eventosPersistidos[1].eventType)
        assertEquals("DinheiroSacado", eventosPersistidos[2].eventType)
        assertEquals("ContaEncerrada", eventosPersistidos.last().eventType)

        assertEquals(1L, eventosPersistidos[0].aggregateVersion)
        assertEquals(2L, eventosPersistidos[1].aggregateVersion)
        assertEquals(3L, eventosPersistidos[2].aggregateVersion)
        assertEquals(4L, eventosPersistidos[3].aggregateVersion)

        val contaReidratada = repository.buscarPorId(idConta)
        assertNotNull(contaReidratada)
        assertEquals(4L, contaReidratada.versaoAtual)
        val estadoFinal = contaReidratada.estadoAtual()
        assertNotNull(estadoFinal)
        assertEquals(0, BigDecimal.ZERO.compareTo(estadoFinal.saldo))
        assertFalse(estadoFinal.ativa)
        assertTrue(estadoFinal.encerrada)
    }
}