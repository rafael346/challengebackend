package com.verzel.challengebackend.service.tmdb;

public class TmdbIntegrationException extends RuntimeException {

    public TmdbIntegrationException(String message) {
        super(message);
    }

    public TmdbIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
