package com.verzel.challengebackend.web;

import com.verzel.challengebackend.security.JwtAuthenticationToken;
import com.verzel.challengebackend.security.JwtService;
import com.verzel.challengebackend.service.EventoService;
import com.verzel.challengebackend.web.dto.EventoRequest;
import com.verzel.challengebackend.web.dto.EventoResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
public class EventoController {

    private final EventoService eventoService;

    public EventoController(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    @GetMapping("/eventos")
    public Flux<EventoResponse> listar() {
        return eventoService.listar().map(EventoResponse::from);
    }

    @GetMapping("/eventos/{id}")
    public Mono<EventoResponse> buscarPorId(@PathVariable UUID id) {
        return eventoService.buscarPorId(id).map(EventoResponse::from);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/eventos")
    public Mono<EventoResponse> criar(@Valid @RequestBody EventoRequest request) {
        return currentUserId()
                .flatMap(organizerId -> eventoService.criar(request, organizerId))
                .map(EventoResponse::from);
    }

    @PutMapping("/eventos/{id}")
    public Mono<EventoResponse> editar(@PathVariable UUID id, @Valid @RequestBody EventoRequest request) {
        return currentUserId()
                .flatMap(organizerId -> eventoService.editar(id, request, organizerId))
                .map(EventoResponse::from);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/eventos/{id}")
    public Mono<Void> remover(@PathVariable UUID id) {
        return currentUserId()
                .flatMap(organizerId -> eventoService.remover(id, organizerId));
    }

    private Mono<UUID> currentUserId() {
        return ReactiveSecurityContextHolder.getContext()
                .map(context -> (JwtAuthenticationToken) context.getAuthentication())
                .map(auth -> ((JwtService.ParsedToken) auth.getPrincipal()).userId());
    }
}
