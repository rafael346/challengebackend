package com.verzel.challengebackend.service.tmdb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TmdbSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(TmdbSyncScheduler.class);

    private final TmdbSyncService tmdbSyncService;

    public TmdbSyncScheduler(TmdbSyncService tmdbSyncService) {
        this.tmdbSyncService = tmdbSyncService;
    }

    @Scheduled(cron = "${tmdb.sync.cron}")
    public void executarSincronizacao() {
        tmdbSyncService.sincronizar().subscribe(
                v -> { },
                erro -> log.error("Erro inesperado ao executar a sincronização com o TMDB", erro));
    }
}
