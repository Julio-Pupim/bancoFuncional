package br.com.funcional.banco.infra.mapper

import br.com.funcional.banco.domain.eventos.*
import br.com.funcional.banco.infra.models.EventoPersistido
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue

@Component
class EventoMapper(
    val mapper: ObjectMapper = jacksonObjectMapper()
) {
    private fun mapToContaAberta(eventoPersistido: EventoPersistido): ContaAberta {
        return mapper.readValue<ContaAberta>(eventoPersistido.payload)
    }

    private fun mapToDinheiroDepositado(eventoPersistido: EventoPersistido): DinheiroDepositado {
        return mapper.readValue<DinheiroDepositado>(eventoPersistido.payload)
    }

    private fun mapToDinheiroSacado(eventoPersistido: EventoPersistido): DinheiroSacado {
        return mapper.readValue<DinheiroSacado>(eventoPersistido.payload)
    }

    private fun mapToTransferenciaRealizada(eventoPersistido: EventoPersistido): TransferenciaRealizada {
        return mapper.readValue<TransferenciaRealizada>(eventoPersistido.payload)
    }

    private fun mapToContaBloqueada(eventoPersistido: EventoPersistido): ContaBloqueada {
        return mapper.readValue<ContaBloqueada>(eventoPersistido.payload)
    }

    private fun mapToContaEncerrada(eventoPersistido: EventoPersistido): ContaEncerrada {
        return mapper.readValue<ContaEncerrada>(eventoPersistido.payload)
    }

    fun mapEventoPersistidoToContaEvento(eventoPersistido: EventoPersistido): ContaEvento {
        return when (eventoPersistido.eventType) {
            "ContaAberta" -> mapToContaAberta(eventoPersistido)
            "DinheiroDepositado" -> mapToDinheiroDepositado(eventoPersistido)
            "DinheiroSacado" -> mapToDinheiroSacado(eventoPersistido)
            "TransferenciaRealizada" -> mapToTransferenciaRealizada(eventoPersistido)
            "ContaBloqueada" -> mapToContaBloqueada(eventoPersistido)
            "ContaEncerrada" -> mapToContaEncerrada(eventoPersistido)
            else -> throw IllegalArgumentException("Invalid event type")
        }
    }
}