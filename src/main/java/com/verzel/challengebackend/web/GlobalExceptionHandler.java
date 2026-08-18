package com.verzel.challengebackend.web;

import com.verzel.challengebackend.service.exception.EventoAccessDeniedException;
import com.verzel.challengebackend.service.exception.EventoNotFoundException;
import com.verzel.challengebackend.service.exception.InvalidCredentialsException;
import com.verzel.challengebackend.service.exception.InvalidEventoException;
import com.verzel.challengebackend.web.dto.ErrorResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex, ServerWebExchange exchange) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(401, "Unauthorized", ex.getMessage(), exchange.getRequest().getPath().value()));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ErrorResponse> handleValidation(WebExchangeBindException ex, ServerWebExchange exchange) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getFieldErrors().forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, "Bad Request", "Dados inválidos", exchange.getRequest().getPath().value(),
                        fieldErrors));
    }

    @ExceptionHandler(EventoNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEventoNotFound(EventoNotFoundException ex, ServerWebExchange exchange) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(404, "Not Found", ex.getMessage(), exchange.getRequest().getPath().value()));
    }

    @ExceptionHandler(EventoAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleEventoAccessDenied(EventoAccessDeniedException ex, ServerWebExchange exchange) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(403, "Forbidden", ex.getMessage(), exchange.getRequest().getPath().value()));
    }

    @ExceptionHandler(InvalidEventoException.class)
    public ResponseEntity<ErrorResponse> handleInvalidEvento(InvalidEventoException ex, ServerWebExchange exchange) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, "Bad Request", ex.getMessage(), exchange.getRequest().getPath().value()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex, ServerWebExchange exchange) {
        HttpStatusCode statusCode = ex.getStatusCode();
        HttpStatus resolved = HttpStatus.resolve(statusCode.value());
        String reason = resolved != null ? resolved.getReasonPhrase() : "Error";
        String message = ex.getReason() != null ? ex.getReason() : reason;
        return ResponseEntity.status(statusCode)
                .body(new ErrorResponse(statusCode.value(), reason, message, exchange.getRequest().getPath().value()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, ServerWebExchange exchange) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(500, "Internal Server Error", "Erro interno inesperado",
                        exchange.getRequest().getPath().value()));
    }
}
