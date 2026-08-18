package com.verzel.challengebackend.service.exception;

public class ReservaAccessDeniedException extends RuntimeException {
    public ReservaAccessDeniedException() {
        super("Você não tem permissão para acessar esta reserva");
    }

    public ReservaAccessDeniedException(String message) {
        super(message);
    }
}
