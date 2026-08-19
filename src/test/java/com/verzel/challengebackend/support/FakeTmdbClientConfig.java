package com.verzel.challengebackend.support;

import com.verzel.challengebackend.service.tmdb.TmdbClient;
import com.verzel.challengebackend.service.tmdb.TmdbMovieDto;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Flux;

@TestConfiguration(proxyBeanMethods = false)
public class FakeTmdbClientConfig {

    public static final TmdbMovieDto FILME_FIXO = new TmdbMovieDto(555001, "Filme Real de Teste",
            "Sinopse de teste vinda do TMDB", "/poster-teste.jpg", "2026-08-01");

    @Bean
    @Primary
    public TmdbClient tmdbClient() {
        return new TmdbClient("http://tmdb-fake.invalid", "chave-fake") {
            @Override
            public Flux<TmdbMovieDto> buscarEmCartaz() {
                return Flux.just(FILME_FIXO);
            }
        };
    }
}
