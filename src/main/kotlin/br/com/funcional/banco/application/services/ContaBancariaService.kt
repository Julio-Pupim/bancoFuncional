package br.com.funcional.banco.application.services

import br.com.funcional.banco.application.repository.ContaBancariaRepository
import br.com.funcional.banco.domain.commands.CommandAceito
import br.com.funcional.banco.domain.commands.ContaCommands
import br.com.funcional.banco.domain.commands.ResultadoCommand
import br.com.funcional.banco.domain.models.ContaBancaria
import org.springframework.stereotype.Service

@Service
class ContaBancariaService(
    val repository: ContaBancariaRepository
) {

    fun novoComando(comando: ContaCommands): ResultadoCommand {

        val contaBancaria = repository.buscarPorId(comando.idConta) ?: ContaBancaria()

        val resultado = contaBancaria.processar(comando)

        if (resultado is CommandAceito) {
            repository.salvar(
                comando.idConta, resultado.eventos,
                contaBancaria.versaoAtual
            )
        }
        return resultado
    }


}