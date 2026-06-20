package br.com.funcional.banco.domain.exception

class PayloadException(msg: String, e: Throwable) : RuntimeException(msg, e)