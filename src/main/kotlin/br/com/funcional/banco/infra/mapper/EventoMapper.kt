package br.com.funcional.banco.infra.mapper

import br.com.funcional.banco.domain.eventos.*
import br.com.funcional.banco.domain.exception.PayloadException
import br.com.funcional.banco.infra.models.EventoPersistido
import org.springframework.stereotype.Component
import tools.jackson.core.exc.StreamReadException
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue

@Component
class EventoMapper(
    val mapper: ObjectMapper = jacksonObjectMapper()
) {
    private inline fun <reified T : ContaEvento> map(
        eventoPersistido: EventoPersistido
    ): T {
        return try {
            mapper.readValue(eventoPersistido.payload)
        } catch (e: StreamReadException) {
            throw PayloadException(
                """Erro ao desserializar ${eventoPersistido.eventType}
                    |payload: ${eventoPersistido.payload}
                    |erro: ${e.message}
                """.trimMargin(),
                e
            )
        }
    }

    fun mapEventoPersistidoToContaEvento(eventoPersistido: EventoPersistido): ContaEvento {
        return when (eventoPersistido.eventType) {
            "ContaAberta" -> map<ContaAberta>(eventoPersistido)
            "DinheiroDepositado" -> map<DinheiroDepositado>(eventoPersistido)
            "DinheiroSacado" -> map<DinheiroSacado>(eventoPersistido)
            "TransferenciaRealizada" -> map<TransferenciaRealizada>(eventoPersistido)
            "ContaBloqueada" -> map<ContaBloqueada>(eventoPersistido)
            "ContaEncerrada" -> map<ContaEncerrada>(eventoPersistido)
            else -> throw IllegalArgumentException("tipo de evento inválido")
        }
    }
}