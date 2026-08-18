package com.verzel.challengebackend.service.exception;

public class EventoNotFoundException extends RuntimeException {
    public EventoNotFoundException() {
        super("Evento não encontrado");
    }
}
