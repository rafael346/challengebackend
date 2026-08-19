package com.verzel.challengebackend.service.tmdb;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class TmdbClient {

    private final WebClient webClient;
    private final String apiKey;

    public TmdbClient(@Value("${tmdb.base-url}") String baseUrl, @Value("${tmdb.api-key}") String apiKey) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    public Flux<TmdbMovieDto> buscarEmCartaz() {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/movie/now_playing")
                        .queryParam("api_key", apiKey)
                        .queryParam("region", "BR")
                        .queryParam("language", "pt-BR")
                        .queryParam("page", 1)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .flatMap(corpo -> Mono.error(new TmdbIntegrationException(
                                "TMDB retornou erro " + response.statusCode().value() + ": " + corpo))))
                .bodyToMono(TmdbNowPlayingResponse.class)
                .timeout(Duration.ofSeconds(5))
                .onErrorMap(ex -> !(ex instanceof TmdbIntegrationException),
                        ex -> new TmdbIntegrationException("Falha ao chamar o TMDB: " + ex.getMessage(), ex))
                .flatMapMany(resposta -> Flux.fromIterable(resposta.results()));
    }
}
