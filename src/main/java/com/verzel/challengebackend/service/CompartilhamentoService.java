package com.verzel.challengebackend.service;

import com.verzel.challengebackend.domain.Ingresso;
import com.verzel.challengebackend.domain.StatusIngresso;
import com.verzel.challengebackend.repository.EventoRepository;
import com.verzel.challengebackend.repository.IngressoRepository;
import com.verzel.challengebackend.service.exception.IngressoNotFoundException;
import com.verzel.challengebackend.service.exception.InvalidReservaException;
import com.verzel.challengebackend.service.exception.ReservaAccessDeniedException;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
public class CompartilhamentoService {

    private final IngressoRepository ingressoRepository;
    private final EventoRepository eventoRepository;

    public CompartilhamentoService(IngressoRepository ingressoRepository, EventoRepository eventoRepository) {
        this.ingressoRepository = ingressoRepository;
        this.eventoRepository = eventoRepository;
    }

    /**
     * Gera (ou reaproveita) o token de compartilhamento público de um ingresso. Idempotente:
     * uma segunda chamada devolve o mesmo token já gerado, em vez de trocar o link em
     * circulação (não há endpoint de revogação — ver spec).
     */
    @Transactional
    public Mono<UUID> compartilhar(UUID ingressoId, UUID compradorId) {
        return ingressoRepository.findById(ingressoId)
                .switchIfEmpty(Mono.error(new IngressoNotFoundException()))
                .flatMap(ingresso -> {
                    if (!ingresso.getCompradorId().equals(compradorId)) {
                        return Mono.error(new ReservaAccessDeniedException(
                                "Você não tem permissão para compartilhar este ingresso"));
                    }
                    if (ingresso.getStatus() != StatusIngresso.VENDIDO) {
                        return Mono
                                .error(new InvalidReservaException("só é possível compartilhar um ingresso vendido"));
                    }
                    if (ingresso.getCompartilhamentoToken() != null) {
                        return Mono.just(ingresso.getCompartilhamentoToken());
                    }
                    UUID token = UUID.randomUUID();
                    return ingressoRepository.save(ingresso.comCompartilhamentoToken(token, OffsetDateTime.now()))
                            .map(Ingresso::getCompartilhamentoToken);
                });
    }

    /** Resolve o ingresso público a partir do token de compartilhamento, com o evento
     * associado (para o nome exibido na página pública). */
    public Mono<IngressoCompartilhado> buscarPorToken(UUID token) {
        return ingressoRepository.findByCompartilhamentoToken(token)
                .switchIfEmpty(Mono.error(new IngressoNotFoundException()))
                .flatMap(ingresso -> eventoRepository.findById(ingresso.getEventoId())
                        .map(evento -> new IngressoCompartilhado(ingresso, evento)));
    }
}
