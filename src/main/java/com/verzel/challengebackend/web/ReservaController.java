package com.verzel.challengebackend.web;

import com.verzel.challengebackend.security.JwtAuthenticationToken;
import com.verzel.challengebackend.security.JwtService;
import com.verzel.challengebackend.service.ReservaService;
import com.verzel.challengebackend.web.dto.ConfirmarReservaRequest;
import com.verzel.challengebackend.web.dto.DisponibilidadeResponse;
import com.verzel.challengebackend.web.dto.IngressoResponse;
import com.verzel.challengebackend.web.dto.ReservaRequest;
import com.verzel.challengebackend.web.dto.ReservaResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @GetMapping("/eventos/{eventoId}/disponibilidade")
    public Mono<DisponibilidadeResponse> disponibilidade(@PathVariable UUID eventoId) {
        return reservaService.disponibilidade(eventoId).map(DisponibilidadeResponse::from);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/eventos/{eventoId}/reservas")
    public Mono<ReservaResponse> criar(@PathVariable UUID eventoId, @Valid @RequestBody ReservaRequest request) {
        return currentUserId()
                .flatMap(compradorId -> reservaService.criar(eventoId, request, compradorId))
                .map(ReservaResponse::from);
    }

    @PostMapping("/reservas/{reservaId}/confirmar")
    public Flux<IngressoResponse> confirmar(@PathVariable UUID reservaId,
            @Valid @RequestBody ConfirmarReservaRequest request) {
        return currentUserId()
                .flatMap(compradorId -> reservaService.confirmar(reservaId, request, compradorId))
                .flatMapMany(Flux::fromIterable)
                .map(IngressoResponse::from);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/reservas/{reservaId}/cancelar")
    public Mono<Void> cancelar(@PathVariable UUID reservaId) {
        return currentUserId()
                .flatMap(compradorId -> reservaService.cancelar(reservaId, compradorId));
    }

    private Mono<UUID> currentUserId() {
        return ReactiveSecurityContextHolder.getContext()
                .map(context -> (JwtAuthenticationToken) context.getAuthentication())
                .map(auth -> ((JwtService.ParsedToken) auth.getPrincipal()).userId());
    }
}
