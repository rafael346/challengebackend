package com.verzel.challengebackend.service.exception;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Email ou senha inválidos");
    }
}
