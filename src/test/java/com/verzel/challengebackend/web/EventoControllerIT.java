package com.verzel.challengebackend.web;

import com.verzel.challengebackend.domain.CategoriaEvento;
import com.verzel.challengebackend.domain.FormaVenda;
import com.verzel.challengebackend.support.TestcontainersConfig;
import com.verzel.challengebackend.web.dto.EventoRequest;
import com.verzel.challengebackend.web.dto.EventoResponse;
import com.verzel.challengebackend.web.dto.LoginRequest;
import com.verzel.challengebackend.web.dto.LoginResponse;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
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
class EventoControllerIT {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void listarEventosNaoRequerAutenticacao() {
        webTestClient.get().uri("/eventos")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void buscarEventoPorIdInexistenteRetorna404() {
        webTestClient.get().uri("/eventos/{id}", UUID.randomUUID())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void listarEventosFuncionaTambemComTokenValido() {
        String token = loginAndGetToken("cliente@verzel.com", "senha123");

        webTestClient.get().uri("/eventos")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void criarEventoComOrganizadorRetorna201ComOEventoCriado() {
        String token = loginAndGetToken("organizador@verzel.com", "senha123");
        EventoRequest request = new EventoRequest("Show de Rock", CategoriaEvento.SHOW, "Um grande show",
                "Arena Central", OffsetDateTime.now().plusDays(30), FormaVenda.ASSENTOS, 10, 15, null,
                new BigDecimal("120.00"));

        webTestClient.post().uri("/eventos")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.titulo").isEqualTo("Show de Rock")
                .jsonPath("$.quantidadeTotalIngressos").isEqualTo(150)
                .jsonPath("$.organizerId").isNotEmpty();
    }

    @Test
    void criarEventoSemTokenRetorna401() {
        webTestClient.post().uri("/eventos")
                .bodyValue(eventoPistaValido())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void criarEventoComTokenDeClienteRetorna403() {
        String token = loginAndGetToken("cliente@verzel.com", "senha123");

        webTestClient.post().uri("/eventos")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .bodyValue(eventoPistaValido())
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void criarEventoComTokenDePortariaRetorna403() {
        String token = loginAndGetToken("portaria@verzel.com", "senha123");

        webTestClient.post().uri("/eventos")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .bodyValue(eventoPistaValido())
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void editarEventoComTokenDePortariaRetorna403() {
        String tokenOrganizador = loginAndGetToken("organizador@verzel.com", "senha123");
        UUID eventoId = criarEventoERetornarId(tokenOrganizador);
        String tokenPortaria = loginAndGetToken("portaria@verzel.com", "senha123");

        webTestClient.put().uri("/eventos/{id}", eventoId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenPortaria)
                .bodyValue(eventoPistaValido())
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void editarEventoSemTokenRetorna401() {
        String tokenOrganizador = loginAndGetToken("organizador@verzel.com", "senha123");
        UUID eventoId = criarEventoERetornarId(tokenOrganizador);

        webTestClient.put().uri("/eventos/{id}", eventoId)
                .bodyValue(eventoPistaValido())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void editarEventoComTokenDeClienteRetorna403() {
        String tokenOrganizador = loginAndGetToken("organizador@verzel.com", "senha123");
        UUID eventoId = criarEventoERetornarId(tokenOrganizador);
        String tokenCliente = loginAndGetToken("cliente@verzel.com", "senha123");

        webTestClient.put().uri("/eventos/{id}", eventoId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenCliente)
                .bodyValue(eventoPistaValido())
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void removerEventoComTokenDePortariaRetorna403() {
        String tokenOrganizador = loginAndGetToken("organizador@verzel.com", "senha123");
        UUID eventoId = criarEventoERetornarId(tokenOrganizador);
        String tokenPortaria = loginAndGetToken("portaria@verzel.com", "senha123");

        webTestClient.delete().uri("/eventos/{id}", eventoId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenPortaria)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void removerEventoSemTokenRetorna401() {
        String tokenOrganizador = loginAndGetToken("organizador@verzel.com", "senha123");
        UUID eventoId = criarEventoERetornarId(tokenOrganizador);

        webTestClient.delete().uri("/eventos/{id}", eventoId)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void removerEventoComTokenDeClienteRetorna403() {
        String tokenOrganizador = loginAndGetToken("organizador@verzel.com", "senha123");
        UUID eventoId = criarEventoERetornarId(tokenOrganizador);
        String tokenCliente = loginAndGetToken("cliente@verzel.com", "senha123");

        webTestClient.delete().uri("/eventos/{id}", eventoId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenCliente)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void criarEventoComTituloVazioRetorna400() {
        String token = loginAndGetToken("organizador@verzel.com", "senha123");
        EventoRequest request = new EventoRequest("", CategoriaEvento.SHOW, "Descrição", "Local",
                OffsetDateTime.now().plusDays(10), FormaVenda.PISTA, null, null, 100, new BigDecimal("50.00"));

        webTestClient.post().uri("/eventos")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void criarEventoComInconsistenciaEntrePistaEAssentosRetorna400() {
        String token = loginAndGetToken("organizador@verzel.com", "senha123");
        EventoRequest request = new EventoRequest("Show", CategoriaEvento.SHOW, "Descrição", "Local",
                OffsetDateTime.now().plusDays(10), FormaVenda.PISTA, 10, 20, null, new BigDecimal("50.00"));

        webTestClient.post().uri("/eventos")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void editarEventoPeloOrganizadorDonoRetorna200ComEventoAtualizado() {
        String token = loginAndGetToken("organizador@verzel.com", "senha123");
        UUID eventoId = criarEventoERetornarId(token);
        EventoRequest requestAtualizado = new EventoRequest("Show Atualizado", CategoriaEvento.SHOW,
                "Descrição atualizada", "Novo local", OffsetDateTime.now().plusDays(40), FormaVenda.PISTA, null,
                null, 700, new BigDecimal("99.90"));

        webTestClient.put().uri("/eventos/{id}", eventoId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .bodyValue(requestAtualizado)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.titulo").isEqualTo("Show Atualizado")
                .jsonPath("$.quantidadeTotalIngressos").isEqualTo(700);
    }

    @Test
    void editarEventoDeOutroOrganizadorRetorna403() {
        String tokenDono = loginAndGetToken("organizador@verzel.com", "senha123");
        UUID eventoId = criarEventoERetornarId(tokenDono);
        String tokenOutroOrganizador = loginAndGetToken("outro-organizador@verzel.com", "senha123");

        webTestClient.put().uri("/eventos/{id}", eventoId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenOutroOrganizador)
                .bodyValue(eventoPistaValido())
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void editarEventoInexistenteRetorna404() {
        String token = loginAndGetToken("organizador@verzel.com", "senha123");

        webTestClient.put().uri("/eventos/{id}", UUID.randomUUID())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .bodyValue(eventoPistaValido())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void removerEventoPeloOrganizadorDonoRetorna204EEventoSomeDaListagem() {
        String token = loginAndGetToken("organizador@verzel.com", "senha123");
        UUID eventoId = criarEventoERetornarId(token);

        webTestClient.delete().uri("/eventos/{id}", eventoId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isNoContent();

        webTestClient.get().uri("/eventos/{id}", eventoId)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void removerEventoDeOutroOrganizadorRetorna403() {
        String tokenDono = loginAndGetToken("organizador@verzel.com", "senha123");
        UUID eventoId = criarEventoERetornarId(tokenDono);
        String tokenOutroOrganizador = loginAndGetToken("outro-organizador@verzel.com", "senha123");

        webTestClient.delete().uri("/eventos/{id}", eventoId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenOutroOrganizador)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void removerEventoInexistenteRetorna404() {
        String token = loginAndGetToken("organizador@verzel.com", "senha123");

        webTestClient.delete().uri("/eventos/{id}", UUID.randomUUID())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isNotFound();
    }

    private EventoRequest eventoPistaValido() {
        return new EventoRequest("Evento de Teste", CategoriaEvento.SHOW, "Descrição", "Local",
                OffsetDateTime.now().plusDays(15), FormaVenda.PISTA, null, null, 200, new BigDecimal("60.00"));
    }

    private UUID criarEventoERetornarId(String token) {
        return webTestClient.post().uri("/eventos")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .bodyValue(eventoPistaValido())
                .exchange()
                .expectStatus().isCreated()
                .expectBody(EventoResponse.class)
                .returnResult()
                .getResponseBody()
                .id();
    }

    private String loginAndGetToken(String email, String senha) {
        return webTestClient.post().uri("/auth/login")
                .bodyValue(new LoginRequest(email, senha))
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginResponse.class)
                .returnResult()
                .getResponseBody()
                .token();
    }
}
