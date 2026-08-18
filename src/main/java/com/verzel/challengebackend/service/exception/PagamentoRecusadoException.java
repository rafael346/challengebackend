package com.verzel.challengebackend.service.exception;

public class PagamentoRecusadoException extends RuntimeException {
    public PagamentoRecusadoException(String motivo) {
        super(motivo);
    }
}
