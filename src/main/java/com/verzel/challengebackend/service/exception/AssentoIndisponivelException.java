package com.verzel.challengebackend.service.exception;

public class AssentoIndisponivelException extends RuntimeException {
    public AssentoIndisponivelException() {
        super("Assento já reservado ou vendido");
    }
}
