package br.com.funcional.banco.infra.models

import java.time.LocalDateTime
import java.util.*

data class EventoPersistido(
    val eventId: UUID,
    val aggregateId: UUID,
    val aggregateType: String,
    val aggregateVersion: Long,
    val eventType: String,
    val schemaVersion: Int,
    val occurredAt: LocalDateTime?,
    val payload: String
)