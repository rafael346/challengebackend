package com.verzel.challengebackend.service.tmdb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TmdbSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(TmdbSyncScheduler.class);

    private final TmdbSyncService tmdbSyncService;
    private final boolean sincronizarNoStartup;

    public TmdbSyncScheduler(TmdbSyncService tmdbSyncService,
            @Value("${tmdb.sync.on-startup}") boolean sincronizarNoStartup) {
        this.tmdbSyncService = tmdbSyncService;
        this.sincronizarNoStartup = sincronizarNoStartup;
    }

    @Scheduled(cron = "${tmdb.sync.cron}")
    public void executarSincronizacao() {
        tmdbSyncService.sincronizar().subscribe(
                v -> { },
                erro -> log.error("Erro inesperado ao executar a sincronização com o TMDB", erro));
    }

    @EventListener(ApplicationReadyEvent.class)
    public void executarSincronizacaoNoStartup() {
        if (!sincronizarNoStartup) {
            return;
        }
        log.info("Executando sincronização com o TMDB no startup da aplicação.");
        executarSincronizacao();
    }
}
