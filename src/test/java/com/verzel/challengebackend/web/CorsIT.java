package com.verzel.challengebackend.web;

import com.verzel.challengebackend.support.TestcontainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Import(TestcontainersConfig.class)
class CorsIT {

    private static final String ORIGEM_PERMITIDA = "http://localhost:3000";
    private static final String ORIGEM_NAO_PERMITIDA = "http://evil.example.com";

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void preflightDeOrigemPermitidaRespondeOkSemExigirAutenticacao() {
        webTestClient.options().uri("/eventos")
                .header(HttpHeaders.ORIGIN, ORIGEM_PERMITIDA)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEM_PERMITIDA)
                .expectHeader().value(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, methods -> {
                    if (methods == null || !methods.contains("POST")) {
                        throw new AssertionError(
                                "Esperava 'POST' em Access-Control-Allow-Methods, encontrado: " + methods);
                    }
                });
    }

    @Test
    void requisicaoRealDeOrigemPermitidaRecebeOHeaderDeCors() {
        webTestClient.get().uri("/eventos")
                .header(HttpHeaders.ORIGIN, ORIGEM_PERMITIDA)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEM_PERMITIDA);
    }

    @Test
    void requisicaoDeOrigemNaoPermitidaEBloqueadaPeloServidor() {
        webTestClient.get().uri("/eventos")
                .header(HttpHeaders.ORIGIN, ORIGEM_NAO_PERMITIDA)
                .exchange()
                .expectStatus().isForbidden()
                .expectHeader().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
    }
}
