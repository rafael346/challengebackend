package com.verzel.challengebackend.service.exception;

public class IngressoNotFoundException extends RuntimeException {
    public IngressoNotFoundException() {
        super("Ingresso não encontrado");
    }
}
