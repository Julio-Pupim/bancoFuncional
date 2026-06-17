package br.com.funcional.banco.application.services

import br.com.funcional.banco.application.repository.ContaBancariaRepository
import br.com.funcional.banco.domain.commands.CommandAceito
import br.com.funcional.banco.domain.commands.ContaCommands
import br.com.funcional.banco.domain.commands.ResultadoCommand
import br.com.funcional.banco.domain.models.ContaBancaria
import br.com.funcional.banco.domain.ports.MetadadosEvento
import org.springframework.stereotype.Service
import java.util.*

@Service
class ContaBancariaService(
    val repository: ContaBancariaRepository
) {

    fun novoComando(comando: ContaCommands): ResultadoCommand {

        val contaBancaria = repository.buscarPorId(comando.idConta) ?: ContaBancaria()

        val resultado = contaBancaria.processar(comando)

        if (resultado is CommandAceito) {
            val metadadoEvento = MetadadosEvento(UUID.randomUUID(), comando.causationId)
            repository.salvar(
                comando.idConta, resultado.eventos,
                contaBancaria.versaoAtual, metadadoEvento
            )
        }
        return resultado
    }


}