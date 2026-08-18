package com.verzel.challengebackend.service.exception;

public class EventoAccessDeniedException extends RuntimeException {
    public EventoAccessDeniedException() {
        super("Você não tem permissão para alterar este evento");
    }
}
