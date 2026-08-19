package com.verzel.challengebackend.service.tmdb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.verzel.challengebackend.domain.CategoriaEvento;
import com.verzel.challengebackend.domain.Evento;
import com.verzel.challengebackend.domain.FormaVenda;
import com.verzel.challengebackend.repository.EventoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class TmdbSyncServiceTest {

    private static final UUID ORGANIZER_ID = UUID.randomUUID();

    private TmdbClient tmdbClient;
    private EventoRepository eventoRepository;
    private TmdbSyncService tmdbSyncService;

    @BeforeEach
    void setUp() {
        tmdbClient = mock(TmdbClient.class);
        eventoRepository = mock(EventoRepository.class);
        tmdbSyncService = new TmdbSyncService(tmdbClient, eventoRepository, "20:00", new BigDecimal("30.00"),
                "Sala a definir", 10, 10, ORGANIZER_ID.toString());
    }

    @Test
    void sincronizarCriaEventoComOsCamposMapeadosDoTmdb() {
        TmdbMovieDto filme = new TmdbMovieDto(101, "Filme Um", "Sinopse do filme", "/poster1.jpg", "2026-08-01");
        when(tmdbClient.buscarEmCartaz()).thenReturn(Flux.just(filme));
        when(eventoRepository.existsByTmdbId(101)).thenReturn(Mono.just(false));
        when(eventoRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(tmdbSyncService.sincronizar()).verifyComplete();

        ArgumentCaptor<Evento> captor = ArgumentCaptor.forClass(Evento.class);
        verify(eventoRepository).save(captor.capture());
        Evento evento = captor.getValue();
        assertThat(evento.getTitulo()).isEqualTo("Filme Um");
        assertThat(evento.getDescricao()).isEqualTo("Sinopse do filme");
        assertThat(evento.getCategoria()).isEqualTo(CategoriaEvento.FILME);
        assertThat(evento.getFormaVenda()).isEqualTo(FormaVenda.ASSENTOS);
        assertThat(evento.getFileiras()).isEqualTo(10);
        assertThat(evento.getColunas()).isEqualTo(10);
        assertThat(evento.getQuantidadeTotalIngressos()).isEqualTo(100);
        assertThat(evento.getPreco()).isEqualByComparingTo("30.00");
        assertThat(evento.getLocal()).isEqualTo("Sala a definir");
        assertThat(evento.getOrganizerId()).isEqualTo(ORGANIZER_ID);
        assertThat(evento.getTmdbId()).isEqualTo(101);
        assertThat(evento.getPosterUrl()).isEqualTo("https://image.tmdb.org/t/p/w500/poster1.jpg");
        assertThat(evento.getDataHora()).isEqualTo(
                OffsetDateTime.of(LocalDate.of(2026, 8, 1), LocalTime.of(20, 0), evento.getDataHora().getOffset()));
    }

    @Test
    void sincronizarPulaFilmeQueJaFoiSincronizadoAntes() {
        TmdbMovieDto filme = new TmdbMovieDto(101, "Filme Um", "Sinopse", "/p.jpg", "2026-08-01");
        when(tmdbClient.buscarEmCartaz()).thenReturn(Flux.just(filme));
        when(eventoRepository.existsByTmdbId(101)).thenReturn(Mono.just(true));

        StepVerifier.create(tmdbSyncService.sincronizar()).verifyComplete();

        verify(eventoRepository, never()).save(any());
    }

    @Test
    void sincronizarUsaDescricaoPadraoQuandoOverviewVemNuloOuVazio() {
        TmdbMovieDto filme = new TmdbMovieDto(101, "Filme Um", null, "/p.jpg", "2026-08-01");
        when(tmdbClient.buscarEmCartaz()).thenReturn(Flux.just(filme));
        when(eventoRepository.existsByTmdbId(101)).thenReturn(Mono.just(false));
        when(eventoRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(tmdbSyncService.sincronizar()).verifyComplete();

        ArgumentCaptor<Evento> captor = ArgumentCaptor.forClass(Evento.class);
        verify(eventoRepository).save(captor.capture());
        assertThat(captor.getValue().getDescricao()).isNotBlank();
    }

    @Test
    void sincronizarUsaDataHoraDeFallbackQuandoReleaseDateVemInvalido() {
        TmdbMovieDto filme = new TmdbMovieDto(101, "Filme Um", "Sinopse", "/p.jpg", "data-invalida");
        when(tmdbClient.buscarEmCartaz()).thenReturn(Flux.just(filme));
        when(eventoRepository.existsByTmdbId(101)).thenReturn(Mono.just(false));
        when(eventoRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(tmdbSyncService.sincronizar()).verifyComplete();

        ArgumentCaptor<Evento> captor = ArgumentCaptor.forClass(Evento.class);
        verify(eventoRepository).save(captor.capture());
        assertThat(captor.getValue().getDataHora()).isAfter(OffsetDateTime.now());
    }

    @Test
    void sincronizarContinuaProcessandoOsDemaisFilmesQuandoUmDelesFalha() {
        TmdbMovieDto filmeComErro = new TmdbMovieDto(101, "Filme Com Erro", "Sinopse", "/p.jpg", "2026-08-01");
        TmdbMovieDto filmeOk = new TmdbMovieDto(102, "Filme Ok", "Sinopse", "/p.jpg", "2026-08-01");
        when(tmdbClient.buscarEmCartaz()).thenReturn(Flux.just(filmeComErro, filmeOk));
        when(eventoRepository.existsByTmdbId(101)).thenReturn(Mono.error(new RuntimeException("falha de banco")));
        when(eventoRepository.existsByTmdbId(102)).thenReturn(Mono.just(false));
        when(eventoRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(tmdbSyncService.sincronizar()).verifyComplete();

        ArgumentCaptor<Evento> captor = ArgumentCaptor.forClass(Evento.class);
        verify(eventoRepository).save(captor.capture());
        assertThat(captor.getValue().getTmdbId()).isEqualTo(102);
    }

    @Test
    void sincronizarNaoPropagaErroQuandoOClienteTmdbFalha() {
        when(tmdbClient.buscarEmCartaz()).thenReturn(Flux.error(new TmdbIntegrationException("TMDB fora do ar")));

        StepVerifier.create(tmdbSyncService.sincronizar()).verifyComplete();

        verify(eventoRepository, never()).save(any());
    }

    @Test
    void sincronizarContabilizaFilmeComErroNoResumoDeLog() {
        ch.qos.logback.classic.Logger logbackLogger =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(TmdbSyncService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logbackLogger.addAppender(appender);

        try {
            TmdbMovieDto filmeComErro = new TmdbMovieDto(101, "Filme Com Erro", "Sinopse", "/p.jpg", "2026-08-01");
            TmdbMovieDto filmeOk = new TmdbMovieDto(102, "Filme Ok", "Sinopse", "/p.jpg", "2026-08-01");
            when(tmdbClient.buscarEmCartaz()).thenReturn(Flux.just(filmeComErro, filmeOk));
            when(eventoRepository.existsByTmdbId(101))
                    .thenReturn(Mono.error(new RuntimeException("falha de banco")));
            when(eventoRepository.existsByTmdbId(102)).thenReturn(Mono.just(false));
            when(eventoRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

            StepVerifier.create(tmdbSyncService.sincronizar()).verifyComplete();

            String resumo = appender.list.stream()
                    .filter(evento -> evento.getLevel() == Level.INFO)
                    .map(ILoggingEvent::getFormattedMessage)
                    .filter(mensagem -> mensagem.contains("Sincronização TMDB concluída"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Log de resumo da sincronização não foi encontrado"));

            assertThat(resumo).contains("2 filmes recebidos");
            assertThat(resumo).contains("1 criados");
            assertThat(resumo).contains("0 pulados");
            assertThat(resumo).contains("1 com erro");
        } finally {
            logbackLogger.detachAppender(appender);
        }
    }
}
