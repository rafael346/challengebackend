package com.verzel.challengebackend.service.exception;

public class ReservaNotFoundException extends RuntimeException {
    public ReservaNotFoundException() {
        super("Reserva não encontrada");
    }
}
