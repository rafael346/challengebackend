package com.verzel.challengebackend.service.exception;

public class QuantidadeIndisponivelException extends RuntimeException {
    public QuantidadeIndisponivelException() {
        super("Quantidade de ingressos indisponível");
    }
}
