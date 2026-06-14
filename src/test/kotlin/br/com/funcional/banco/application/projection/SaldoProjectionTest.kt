package br.com.funcional.banco.application.projection

import br.com.funcional.banco.application.query.ConsultaSaldo
import br.com.funcional.banco.application.query.ConsultarSaldoHandler
import br.com.funcional.banco.domain.eventos.ContaAberta
import br.com.funcional.banco.domain.eventos.DinheiroDepositado
import br.com.funcional.banco.domain.eventos.DinheiroSacado
import br.com.funcional.banco.domain.eventos.TransferenciaRealizada
import br.com.funcional.banco.infra.mock.InMemorySaldoRepository
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.*
import kotlin.test.assertEquals

class SaldoProjectionTest {

    private val repository = InMemorySaldoRepository()

    @Test
    fun `deve projetar saldo final da conta`() {

        val projection = SaldoProjection(repository)

        val contaId = UUID.randomUUID()

        projection.handle(
            ContaAberta(
                id = contaId,
                saldoInicial = BigDecimal("100")
            )
        )

        projection.handle(
            DinheiroDepositado(
                idConta = contaId,
                valor = BigDecimal("50")
            )
        )

        projection.handle(
            DinheiroSacado(
                idConta = contaId,
                valor = BigDecimal("20")
            )
        )

        assertEquals(
            BigDecimal("130"),
            repository.buscarSaldo(contaId)
        )
    }

    @Test
    fun `deve atualizar saldo das duas contas ao projetar transferencia`() {

        val projection = SaldoProjection(repository)

        val origem = UUID.randomUUID()
        val destino = UUID.randomUUID()

        projection.handle(
            ContaAberta(origem, BigDecimal("100"))
        )

        projection.handle(
            ContaAberta(destino, BigDecimal("50"))
        )

        projection.handle(
            TransferenciaRealizada(
                idContaOrigem = origem,
                idContaDestino = destino,
                valor = BigDecimal("30")
            )
        )

        assertEquals(
            BigDecimal("70"),
            repository.buscarSaldo(origem)
        )

        assertEquals(
            BigDecimal("80"),
            repository.buscarSaldo(destino)
        )
    }

    @Test
    fun `deve consultar saldo projetado`() {


        val contaId = UUID.randomUUID()

        repository.salvar(
            contaId,
            BigDecimal("500")
        )

        val handler = ConsultarSaldoHandler(repository)

        val saldo = handler.handle(
            ConsultaSaldo(contaId)
        )

        assertEquals(
            BigDecimal("500"),
            saldo
        )
    }

    @Test
    fun `deve reconstruir read model a partir dos eventos`() {
        val contaId = UUID.randomUUID()

        val eventos = listOf(
            ContaAberta(
                id = contaId,
                saldoInicial = BigDecimal("100")
            ),
            DinheiroDepositado(
                idConta = contaId,
                valor = BigDecimal("50")
            ),
            DinheiroSacado(
                idConta = contaId,
                valor = BigDecimal("20")
            )
        )

        val projection = SaldoProjection(repository)

        eventos.forEach(projection::handle)

        assertEquals(
            BigDecimal("130"),
            repository.buscarSaldo(contaId)
        )
    }

}