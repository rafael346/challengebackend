package com.verzel.challengebackend.service.tmdb;

import com.verzel.challengebackend.domain.CategoriaEvento;
import com.verzel.challengebackend.domain.Evento;
import com.verzel.challengebackend.domain.FormaVenda;
import com.verzel.challengebackend.repository.EventoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class TmdbSyncService {

    private static final Logger log = LoggerFactory.getLogger(TmdbSyncService.class);
    private static final String DESCRICAO_PADRAO = "Sinopse não disponível.";

    private final TmdbClient tmdbClient;
    private final EventoRepository eventoRepository;
    private final LocalTime horarioSessaoPadrao;
    private final BigDecimal precoPadrao;
    private final String localPadrao;
    private final Integer fileirasPadrao;
    private final Integer colunasPadrao;
    private final UUID organizerId;

    public TmdbSyncService(TmdbClient tmdbClient, EventoRepository eventoRepository,
            @Value("${tmdb.sync.default-session-time}") String horarioSessaoPadrao,
            @Value("${tmdb.sync.default-preco}") BigDecimal precoPadrao,
            @Value("${tmdb.sync.default-local}") String localPadrao,
            @Value("${tmdb.sync.default-fileiras}") Integer fileirasPadrao,
            @Value("${tmdb.sync.default-colunas}") Integer colunasPadrao,
            @Value("${tmdb.sync.organizer-id}") String organizerId) {
        this.tmdbClient = tmdbClient;
        this.eventoRepository = eventoRepository;
        this.horarioSessaoPadrao = LocalTime.parse(horarioSessaoPadrao);
        this.precoPadrao = precoPadrao;
        this.localPadrao = localPadrao;
        this.fileirasPadrao = fileirasPadrao;
        this.colunasPadrao = colunasPadrao;
        this.organizerId = UUID.fromString(organizerId);
    }

    public Mono<Void> sincronizar() {
        AtomicInteger total = new AtomicInteger();
        AtomicInteger criados = new AtomicInteger();
        AtomicInteger pulados = new AtomicInteger();
        AtomicInteger erros = new AtomicInteger();

        return tmdbClient.buscarEmCartaz()
                .doOnNext(filme -> total.incrementAndGet())
                .concatMap(filme -> processar(filme, criados, pulados, erros))
                .then()
                .doOnSuccess(v -> log.info(
                        "Sincronização TMDB concluída: {} filmes recebidos, {} criados, {} pulados, {} com erro.",
                        total.get(), criados.get(), pulados.get(), erros.get()))
                .onErrorResume(TmdbIntegrationException.class, ex -> {
                    log.warn("Falha ao sincronizar filmes do TMDB: {}", ex.getMessage());
                    return Mono.empty();
                });
    }

    private Mono<Void> processar(TmdbMovieDto filme, AtomicInteger criados, AtomicInteger pulados,
            AtomicInteger erros) {
        return eventoRepository.existsByTmdbId(filme.id())
                .flatMap(jaExiste -> {
                    if (jaExiste) {
                        pulados.incrementAndGet();
                        return Mono.<Void>empty();
                    }
                    return criarEvento(filme)
                            .doOnSuccess(evento -> criados.incrementAndGet())
                            .then();
                })
                .onErrorResume(ex -> {
                    erros.incrementAndGet();
                    log.warn("Falha ao processar o filme TMDB id={} ('{}'): {}", filme.id(), filme.title(),
                            ex.getMessage());
                    return Mono.empty();
                });
    }

    private Mono<Evento> criarEvento(TmdbMovieDto filme) {
        OffsetDateTime agora = OffsetDateTime.now();
        String descricao = (filme.overview() == null || filme.overview().isBlank())
                ? DESCRICAO_PADRAO
                : filme.overview();
        String posterUrl = filme.posterPath() == null ? null : "https://image.tmdb.org/t/p/w500" + filme.posterPath();
        OffsetDateTime dataHora = calcularDataHora(filme.releaseDate());
        int quantidadeTotalIngressos = fileirasPadrao * colunasPadrao;

        Evento evento = new Evento(UUID.randomUUID(), filme.title(), CategoriaEvento.FILME, descricao, localPadrao,
                dataHora, FormaVenda.ASSENTOS, fileirasPadrao, colunasPadrao, quantidadeTotalIngressos, precoPadrao,
                organizerId, agora, agora, filme.id(), posterUrl)
                .marcarComoNovo();

        return eventoRepository.save(evento);
    }

    private OffsetDateTime calcularDataHora(String releaseDate) {
        LocalDate data = parseDataOuNulo(releaseDate);
        if (data == null) {
            data = LocalDate.now().plusDays(1);
        }
        ZoneId zona = ZoneId.systemDefault();
        return OffsetDateTime.of(data, horarioSessaoPadrao, zona.getRules().getOffset(data.atTime(horarioSessaoPadrao)));
    }

    private LocalDate parseDataOuNulo(String releaseDate) {
        if (releaseDate == null || releaseDate.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(releaseDate);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
