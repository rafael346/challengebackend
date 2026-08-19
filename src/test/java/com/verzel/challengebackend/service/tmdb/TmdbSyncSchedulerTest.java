package com.verzel.challengebackend.service.tmdb;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class TmdbSyncSchedulerTest {

    @Test
    void executarSincronizacaoDelegaParaOTmdbSyncService() {
        TmdbSyncService tmdbSyncService = mock(TmdbSyncService.class);
        when(tmdbSyncService.sincronizar()).thenReturn(Mono.empty());
        TmdbSyncScheduler scheduler = new TmdbSyncScheduler(tmdbSyncService, true);

        scheduler.executarSincronizacao();

        verify(tmdbSyncService).sincronizar();
    }

    @Test
    void executarSincronizacaoNoStartupDelegaParaOTmdbSyncServiceQuandoHabilitado() {
        TmdbSyncService tmdbSyncService = mock(TmdbSyncService.class);
        when(tmdbSyncService.sincronizar()).thenReturn(Mono.empty());
        TmdbSyncScheduler scheduler = new TmdbSyncScheduler(tmdbSyncService, true);

        scheduler.executarSincronizacaoNoStartup();

        verify(tmdbSyncService).sincronizar();
    }

    @Test
    void executarSincronizacaoNoStartupNaoFazNadaQuandoDesabilitado() {
        TmdbSyncService tmdbSyncService = mock(TmdbSyncService.class);
        TmdbSyncScheduler scheduler = new TmdbSyncScheduler(tmdbSyncService, false);

        scheduler.executarSincronizacaoNoStartup();

        verify(tmdbSyncService, never()).sincronizar();
    }
}
