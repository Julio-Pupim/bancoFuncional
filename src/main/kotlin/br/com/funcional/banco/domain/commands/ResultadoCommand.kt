package br.com.funcional.banco.domain.commands

import br.com.funcional.banco.domain.eventos.ContaEvento

sealed interface ResultadoCommand

data class CommandAceito(
    val eventos: List<ContaEvento>
) : ResultadoCommand

data class CommandRejeitado(
    val erro: ErroDominio
) : ResultadoCommand