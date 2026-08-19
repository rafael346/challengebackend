package com.verzel.challengebackend.service.tmdb;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class TmdbClientTest {

    private MockWebServer mockWebServer;
    private TmdbClient tmdbClient;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String baseUrl = mockWebServer.url("").toString().replaceAll("/$", "");
        tmdbClient = new TmdbClient(baseUrl, "chave-de-teste");
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void buscarEmCartazMapeiaOsFilmesRetornadosPeloTmdb() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "results": [
                            {
                              "id": 101,
                              "title": "Filme Um",
                              "overview": "Sinopse do filme um",
                              "poster_path": "/poster1.jpg",
                              "release_date": "2026-08-01"
                            },
                            {
                              "id": 102,
                              "title": "Filme Dois",
                              "overview": "Sinopse do filme dois",
                              "poster_path": null,
                              "release_date": "2026-07-15"
                            }
                          ]
                        }
                        """));

        StepVerifier.create(tmdbClient.buscarEmCartaz())
                .assertNext(filme -> {
                    assertThat(filme.id()).isEqualTo(101);
                    assertThat(filme.title()).isEqualTo("Filme Um");
                    assertThat(filme.overview()).isEqualTo("Sinopse do filme um");
                    assertThat(filme.posterPath()).isEqualTo("/poster1.jpg");
                    assertThat(filme.releaseDate()).isEqualTo("2026-08-01");
                })
                .assertNext(filme -> {
                    assertThat(filme.id()).isEqualTo(102);
                    assertThat(filme.posterPath()).isNull();
                })
                .verifyComplete();

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getPath()).contains("/movie/now_playing");
        assertThat(request.getPath()).contains("api_key=chave-de-teste");
        assertThat(request.getPath()).contains("region=BR");
    }

    @Test
    void buscarEmCartazComRespostaDeErroLancaTmdbIntegrationException() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(401).setBody("chave inválida"));

        StepVerifier.create(tmdbClient.buscarEmCartaz())
                .expectErrorMatches(ex -> ex instanceof TmdbIntegrationException
                        && ex.getMessage().contains("401"))
                .verify();
    }

    @Test
    void buscarEmCartazComRespostaSemResultsRetornaFluxVazio() {
        mockWebServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{}"));

        StepVerifier.create(tmdbClient.buscarEmCartaz())
                .verifyComplete();
    }

    @Test
    void buscarEmCartazComFalhaDeRedeLancaTmdbIntegrationException() {
        TmdbClient clienteComPortaInexistente = new TmdbClient("http://localhost:1", "chave-de-teste");

        StepVerifier.create(clienteComPortaInexistente.buscarEmCartaz())
                .expectErrorMatches(ex -> ex instanceof TmdbIntegrationException)
                .verify();
    }
}
