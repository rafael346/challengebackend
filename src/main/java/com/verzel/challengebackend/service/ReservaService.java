package com.verzel.challengebackend.service;

import com.verzel.challengebackend.domain.Evento;
import com.verzel.challengebackend.domain.FormaVenda;
import com.verzel.challengebackend.domain.Ingresso;
import com.verzel.challengebackend.domain.StatusIngresso;
import com.verzel.challengebackend.repository.EventoRepository;
import com.verzel.challengebackend.repository.IngressoRepository;
import com.verzel.challengebackend.service.exception.AssentoIndisponivelException;
import com.verzel.challengebackend.service.exception.EventoNotFoundException;
import com.verzel.challengebackend.service.exception.InvalidReservaException;
import com.verzel.challengebackend.service.exception.QuantidadeIndisponivelException;
import com.verzel.challengebackend.web.dto.AssentoRequest;
import com.verzel.challengebackend.web.dto.ReservaRequest;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ReservaService {

    private final EventoRepository eventoRepository;
    private final IngressoRepository ingressoRepository;
    private final long holdDurationSeconds;

    public ReservaService(EventoRepository eventoRepository, IngressoRepository ingressoRepository,
            @Value("${reserva.hold-duration-seconds}") long holdDurationSeconds) {
        this.eventoRepository = eventoRepository;
        this.ingressoRepository = ingressoRepository;
        this.holdDurationSeconds = holdDurationSeconds;
    }

    
    @Transactional
    public Mono<List<Ingresso>> criar(UUID eventoId, ReservaRequest request, UUID compradorId) {
        return eventoRepository.buscarComLockPorId(eventoId)
                .switchIfEmpty(Mono.error(new EventoNotFoundException()))
                .flatMap(evento -> ingressoRepository.expirarReservasVencidas(eventoId)
                        .then(Mono.defer(() -> reservarItens(evento, request, compradorId))));
    }

    private Mono<List<Ingresso>> reservarItens(Evento evento, ReservaRequest request, UUID compradorId) {
        if (evento.getFormaVenda() == FormaVenda.ASSENTOS) {
            return reservarAssentos(evento, request, compradorId);
        }
        return reservarPista(evento, request, compradorId);
    }

    private Mono<List<Ingresso>> reservarAssentos(Evento evento, ReservaRequest request, UUID compradorId) {
        if (request.assentos() == null || request.assentos().isEmpty()) {
            return Mono.error(new InvalidReservaException("assentos é obrigatório para forma de venda ASSENTOS"));
        }
        if (request.quantidade() != null) {
            return Mono.error(new InvalidReservaException("quantidade não se aplica para forma de venda ASSENTOS"));
        }
        for (AssentoRequest assento : request.assentos()) {
            if (assento.fileira() < 1 || assento.fileira() > evento.getFileiras() || assento.coluna() < 1
                    || assento.coluna() > evento.getColunas()) {
                return Mono.error(new InvalidReservaException(
                        "assento fileira=%d coluna=%d fora do grid do evento".formatted(assento.fileira(),
                                assento.coluna())));
            }
        }
        UUID reservaId = UUID.randomUUID();
        return Flux.fromIterable(request.assentos())
                .concatMap(assento -> ingressoRepository
                        .buscarAssentoAtivo(evento.getId(), assento.fileira(), assento.coluna())
                        .flatMap(ocupado -> Mono.<AssentoRequest>error(new AssentoIndisponivelException()))
                        .switchIfEmpty(Mono.just(assento)))
                .collectList()
                .flatMap(assentosLivres -> ingressoRepository
                        .saveAll(novosIngressosAssentos(evento, compradorId, reservaId, assentosLivres))
                        .collectList());
    }

    private Mono<List<Ingresso>> reservarPista(Evento evento, ReservaRequest request, UUID compradorId) {
        if (request.quantidade() == null || request.quantidade() < 1) {
            return Mono.error(new InvalidReservaException(
                    "quantidade é obrigatória e deve ser positiva para forma de venda PISTA"));
        }
        if (request.assentos() != null && !request.assentos().isEmpty()) {
            return Mono.error(new InvalidReservaException("assentos não se aplica para forma de venda PISTA"));
        }
        UUID reservaId = UUID.randomUUID();
        return ingressoRepository.contarAtivosPorEvento(evento.getId())
                .flatMap(ativos -> {
                    if (ativos + request.quantidade() > evento.getQuantidadeTotalIngressos()) {
                        return Mono.error(new QuantidadeIndisponivelException());
                    }
                    return ingressoRepository
                            .saveAll(novosIngressosPista(evento, compradorId, reservaId, request.quantidade()))
                            .collectList();
                });
    }

    private List<Ingresso> novosIngressosAssentos(Evento evento, UUID compradorId, UUID reservaId,
            List<AssentoRequest> assentos) {
        OffsetDateTime agora = OffsetDateTime.now();
        OffsetDateTime expiraEm = agora.plusSeconds(holdDurationSeconds);
        OffsetDateTime validoAte = evento.getDataHora().plusHours(1);
        return assentos.stream()
                .map(assento -> new Ingresso(UUID.randomUUID(), evento.getId(), reservaId, compradorId,
                        assento.fileira(), assento.coluna(), evento.getPreco(), StatusIngresso.RESERVADO, expiraEm,
                        validoAte, null, agora, agora)
                        .marcarComoNovo())
                .toList();
    }

    private List<Ingresso> novosIngressosPista(Evento evento, UUID compradorId, UUID reservaId, int quantidade) {
        OffsetDateTime agora = OffsetDateTime.now();
        OffsetDateTime expiraEm = agora.plusSeconds(holdDurationSeconds);
        OffsetDateTime validoAte = evento.getDataHora().plusHours(1);
        return IntStream.range(0, quantidade)
                .mapToObj(i -> new Ingresso(UUID.randomUUID(), evento.getId(), reservaId, compradorId, null, null,
                        evento.getPreco(), StatusIngresso.RESERVADO, expiraEm, validoAte, null, agora, agora)
                        .marcarComoNovo())
                .toList();
    }
}
