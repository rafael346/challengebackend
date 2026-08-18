package com.verzel.challengebackend.web;

import com.verzel.challengebackend.service.exception.AssentoIndisponivelException;
import com.verzel.challengebackend.service.exception.EventoAccessDeniedException;
import com.verzel.challengebackend.service.exception.EventoNotFoundException;
import com.verzel.challengebackend.service.exception.InvalidCredentialsException;
import com.verzel.challengebackend.service.exception.InvalidEventoException;
import com.verzel.challengebackend.service.exception.InvalidReservaException;
import com.verzel.challengebackend.service.exception.PagamentoRecusadoException;
import com.verzel.challengebackend.service.exception.QuantidadeIndisponivelException;
import com.verzel.challengebackend.service.exception.ReservaAccessDeniedException;
import com.verzel.challengebackend.service.exception.ReservaExpiradaException;
import com.verzel.challengebackend.service.exception.ReservaNotFoundException;
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

    @ExceptionHandler(InvalidReservaException.class)
    public ResponseEntity<ErrorResponse> handleInvalidReserva(InvalidReservaException ex, ServerWebExchange exchange) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, "Bad Request", ex.getMessage(), exchange.getRequest().getPath().value()));
    }

    @ExceptionHandler(AssentoIndisponivelException.class)
    public ResponseEntity<ErrorResponse> handleAssentoIndisponivel(AssentoIndisponivelException ex,
            ServerWebExchange exchange) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(409, "Conflict", ex.getMessage(), exchange.getRequest().getPath().value()));
    }

    @ExceptionHandler(QuantidadeIndisponivelException.class)
    public ResponseEntity<ErrorResponse> handleQuantidadeIndisponivel(QuantidadeIndisponivelException ex,
            ServerWebExchange exchange) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(409, "Conflict", ex.getMessage(), exchange.getRequest().getPath().value()));
    }

    @ExceptionHandler(ReservaNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleReservaNotFound(ReservaNotFoundException ex, ServerWebExchange exchange) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(404, "Not Found", ex.getMessage(), exchange.getRequest().getPath().value()));
    }

    @ExceptionHandler(ReservaAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleReservaAccessDenied(ReservaAccessDeniedException ex,
            ServerWebExchange exchange) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(403, "Forbidden", ex.getMessage(), exchange.getRequest().getPath().value()));
    }

    @ExceptionHandler(ReservaExpiradaException.class)
    public ResponseEntity<ErrorResponse> handleReservaExpirada(ReservaExpiradaException ex, ServerWebExchange exchange) {
        return ResponseEntity.status(HttpStatus.GONE)
                .body(new ErrorResponse(410, "Gone", ex.getMessage(), exchange.getRequest().getPath().value()));
    }

    @ExceptionHandler(PagamentoRecusadoException.class)
    public ResponseEntity<ErrorResponse> handlePagamentoRecusado(PagamentoRecusadoException ex,
            ServerWebExchange exchange) {
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                .body(new ErrorResponse(402, "Payment Required", ex.getMessage(),
                        exchange.getRequest().getPath().value()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, ServerWebExchange exchange) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(500, "Internal Server Error", "Erro interno inesperado",
                        exchange.getRequest().getPath().value()));
    }
}
