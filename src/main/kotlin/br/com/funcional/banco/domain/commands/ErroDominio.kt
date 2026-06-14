package br.com.funcional.banco.domain.commands

sealed interface ErroDominio

data object ContaJaAberta : ErroDominio
data object ContaNaoAberta : ErroDominio
data object ContaEncerrada : ErroDominio
data object ContaBloqueada : ErroDominio
data object SaldoInsuficiente : ErroDominio
data object ValorInvalido : ErroDominio
data object ContaDestinoInvalida : ErroDominio