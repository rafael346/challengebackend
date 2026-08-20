package com.verzel.challengebackend.web;

import com.verzel.challengebackend.security.JwtAuthenticationToken;
import com.verzel.challengebackend.security.JwtService;
import com.verzel.challengebackend.service.CompartilhamentoService;
import com.verzel.challengebackend.service.ReservaService;
import com.verzel.challengebackend.service.ValidacaoService;
import com.verzel.challengebackend.web.dto.CompartilhamentoResponse;
import com.verzel.challengebackend.web.dto.IngressoPublicoResponse;
import com.verzel.challengebackend.web.dto.IngressoResponse;
import com.verzel.challengebackend.web.dto.ValidacaoResponse;
import java.util.UUID;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
public class IngressoController {

    private final ValidacaoService validacaoService;
    private final CompartilhamentoService compartilhamentoService;
    private final ReservaService reservaService;

    public IngressoController(ValidacaoService validacaoService, CompartilhamentoService compartilhamentoService,
            ReservaService reservaService) {
        this.validacaoService = validacaoService;
        this.compartilhamentoService = compartilhamentoService;
        this.reservaService = reservaService;
    }

    @PostMapping("/eventos/{eventoId}/ingressos/{ingressoId}/validar")
    public Mono<ValidacaoResponse> validar(@PathVariable UUID eventoId, @PathVariable UUID ingressoId) {
        return currentUserId()
                .flatMap(portariaId -> validacaoService.validar(eventoId, ingressoId, portariaId))
                .map(ValidacaoResponse::from);
    }

    /** Lista os ingressos do usuário logado, para a tela "Meus ingressos" do front-end. */
    @GetMapping("/ingressos")
    public Flux<IngressoResponse> meusIngressos() {
        return currentUserId()
                .flatMapMany(reservaService::listarComprados)
                .map(IngressoResponse::from);
    }

    @PostMapping("/ingressos/{ingressoId}/compartilhar")
    public Mono<CompartilhamentoResponse> compartilhar(@PathVariable UUID ingressoId, ServerHttpRequest request) {
        return currentUserId()
                .flatMap(compradorId -> compartilhamentoService.compartilhar(ingressoId, compradorId))
                .map(token -> new CompartilhamentoResponse(linkPublico(request, token)));
    }

    @GetMapping("/ingressos/compartilhados/{token}")
    public Mono<IngressoPublicoResponse> buscarCompartilhado(@PathVariable UUID token) {
        return compartilhamentoService.buscarPorToken(token).map(IngressoPublicoResponse::from);
    }

    private String linkPublico(ServerHttpRequest request, UUID token) {
        return UriComponentsBuilder.fromUri(request.getURI())
                .replacePath("/ingressos/compartilhados/{token}")
                .replaceQuery(null)
                .build(token)
                .toString();
    }

    private Mono<UUID> currentUserId() {
        return ReactiveSecurityContextHolder.getContext()
                .map(context -> (JwtAuthenticationToken) context.getAuthentication())
                .map(auth -> ((JwtService.ParsedToken) auth.getPrincipal()).userId());
    }
}
