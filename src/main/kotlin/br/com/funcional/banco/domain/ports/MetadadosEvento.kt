package br.com.funcional.banco.domain.ports

import java.util.*

data class MetadadosEvento(
    val correlationId: UUID,
    val causationId: UUID,
) {
}