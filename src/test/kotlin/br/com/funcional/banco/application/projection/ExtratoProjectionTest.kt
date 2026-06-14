package br.com.funcional.banco.application.projection

import br.com.funcional.banco.application.query.ExtratoReadModel
import br.com.funcional.banco.domain.eventos.ContaAberta
import br.com.funcional.banco.domain.eventos.DinheiroDepositado
import br.com.funcional.banco.domain.eventos.DinheiroSacado
import br.com.funcional.banco.infra.mock.InMemoryExtratoRepository
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.*
import kotlin.test.assertEquals

class ExtratoProjectionTest {

    private val repository = InMemoryExtratoRepository()

    @Test
    fun `deve projetar extato da conta`() {

        val projection = ExtratoProjection(repository)

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
        val expected = mutableListOf(
            ExtratoReadModel(contaId, "DEPOSITO", BigDecimal("50")),
            ExtratoReadModel(contaId, "SAQUE", BigDecimal("20")),
        )

        assertEquals(
            expected,
            repository.buscarExtrato(contaId)
        )
    }
}