package com.verzel.challengebackend.web.dto;

import java.time.OffsetDateTime;
import java.util.Map;

public record ErrorResponse(
        OffsetDateTime timestamp, int status, String error, String message, String path, Map<String, String> errors) {

    public ErrorResponse(int status, String error, String message, String path) {
        this(OffsetDateTime.now(), status, error, message, path, null);
    }

    public ErrorResponse(int status, String error, String message, String path, Map<String, String> errors) {
        this(OffsetDateTime.now(), status, error, message, path, errors);
    }
}
