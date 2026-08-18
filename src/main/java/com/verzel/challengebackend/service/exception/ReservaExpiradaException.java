package com.verzel.challengebackend.service.exception;

public class ReservaExpiradaException extends RuntimeException {
    public ReservaExpiradaException() {
        super("Reserva expirada ou já finalizada");
    }
}
