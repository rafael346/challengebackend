package com.verzel.challengebackend.web;

import com.verzel.challengebackend.domain.CategoriaEvento;
import com.verzel.challengebackend.domain.FormaVenda;
import com.verzel.challengebackend.support.FakePagamentoGatewayConfig;
import com.verzel.challengebackend.support.TestcontainersConfig;
import com.verzel.challengebackend.web.dto.AssentoRequest;
import com.verzel.challengebackend.web.dto.ConfirmarReservaRequest;
import com.verzel.challengebackend.web.dto.EventoRequest;
import com.verzel.challengebackend.web.dto.EventoResponse;
import com.verzel.challengebackend.web.dto.LoginRequest;
import com.verzel.challengebackend.web.dto.LoginResponse;
import com.verzel.challengebackend.web.dto.ReservaRequest;
import com.verzel.challengebackend.web.dto.ReservaResponse;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Import({TestcontainersConfig.class, FakePagamentoGatewayConfig.class})
@TestPropertySource(properties = "reserva.hold-duration-seconds=3")
class ReservaControllerIT {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void disponibilidadeDeEventoAssentosNaoRequerAutenticacao() {
        String tokenOrganizador = loginAndGetToken("organizador@verzel.com", "senha123");
        UUID eventoId = criarEventoAssentosERetornarId(tokenOrganizador);

        webTestClient.get().uri("/eventos/{id}/disponibilidade", eventoId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.fileiras").isEqualTo(5)
                .jsonPath("$.colunas").isEqualTo(5)
                .jsonPath("$.assentosOcupados").isArray();
    }

    @Test
    void criarReservaDeAssentoComClienteRetorna201() {
        String tokenOrganizador = loginAndGetToken("organizador@verzel.com", "senha123");
        UUID eventoId = criarEventoAssentosERetornarId(tokenOrganizador);
        String tokenCliente = loginAndGetToken("cliente@verzel.com", "senha123");

        webTestClient.post().uri("/eventos/{id}/reservas", eventoId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenCliente)
                .bodyValue(new ReservaRequest(List.of(new AssentoRequest(1, 1)), null))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.reservaId").isNotEmpty()
                .jsonPath("$.itens.length()").isEqualTo(1)
                .jsonPath("$.valorTotal").isEqualTo(120.00);
    }

    @Test
    void criarReservaSemTokenRetorna401() {
        String tokenOrganizador = loginAndGetToken("organizador@verzel.com", "senha123");
        UUID eventoId = criarEventoAssentosERetornarId(tokenOrganizador);

        webTestClient.post().uri("/eventos/{id}/reservas", eventoId)
                .bodyValue(new ReservaRequest(List.of(new AssentoRequest(2, 2)), null))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void criarReservaComTokenDeOrganizadorRetorna403() {
        String tokenOrganizador = loginAndGetToken("organizador@verzel.com", "senha123");
        UUID eventoId = criarEventoAssentosERetornarId(tokenOrganizador);

        webTestClient.post().uri("/eventos/{id}/reservas", eventoId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenOrganizador)
                .bodyValue(new ReservaRequest(List.of(new AssentoRequest(2, 2)), null))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void criarReservaDeAssentoJaOcupadoRetorna409() {
        String tokenOrganizador = loginAndGetToken("organizador@verzel.com", "senha123");
        UUID eventoId = criarEventoAssentosERetornarId(tokenOrganizador);
        String tokenCliente = loginAndGetToken("cliente@verzel.com", "senha123");
        ReservaRequest request = new ReservaRequest(List.of(new AssentoRequest(3, 3)), null);
        webTestClient.post().uri("/eventos/{id}/reservas", eventoId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenCliente)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated();

        String tokenOutraCliente = loginAndGetToken("outra-cliente@verzel.com", "senha123");
        webTestClient.post().uri("/eventos/{id}/reservas", eventoId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenOutraCliente)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void criarReservaDePistaExcedendoCapacidadeRetorna409() {
        String tokenOrganizador = loginAndGetToken("organizador@verzel.com", "senha123");
        UUID eventoId = criarEventoPistaERetornarId(tokenOrganizador, 2);
        String tokenCliente = loginAndGetToken("cliente@verzel.com", "senha123");

        webTestClient.post().uri("/eventos/{id}/reservas", eventoId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenCliente)
                .bodyValue(new ReservaRequest(null, 3))
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void criarReservaDeEventoInexistenteRetorna404() {
        String tokenCliente = loginAndGetToken("cliente@verzel.com", "senha123");

        webTestClient.post().uri("/eventos/{id}/reservas", UUID.randomUUID())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenCliente)
                .bodyValue(new ReservaRequest(null, 1))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void confirmarComPagamentoAprovadoRetorna200EIngressosVendidos() {
        String tokenOrganizador = loginAndGetToken("organizador@verzel.com", "senha123");
        UUID eventoId = criarEventoAssentosERetornarId(tokenOrganizador);
        String tokenCliente = loginAndGetToken("cliente@verzel.com", "senha123");
        UUID reservaId = criarReservaERetornarId(tokenCliente, eventoId, 1, 4);

        webTestClient.post().uri("/reservas/{id}/confirmar", reservaId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenCliente)
                .bodyValue(new ConfirmarReservaRequest(FakePagamentoGatewayConfig.PAYMENT_METHOD_SUCESSO))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].status").isEqualTo("VENDIDO");
    }

    @Test
    void confirmarComPagamentoRecusadoRetorna402() {
        String tokenOrganizador = loginAndGetToken("organizador@verzel.com", "senha123");
        UUID eventoId = criarEventoAssentosERetornarId(tokenOrganizador);
        String tokenCliente = loginAndGetToken("cliente@verzel.com", "senha123");
        UUID reservaId = criarReservaERetornarId(tokenCliente, eventoId, 1, 5);

        webTestClient.post().uri("/reservas/{id}/confirmar", reservaId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenCliente)
                .bodyValue(new ConfirmarReservaRequest(FakePagamentoGatewayConfig.PAYMENT_METHOD_RECUSADO))
                .exchange()
                .expectStatus().isEqualTo(402);
    }

    @Test
    void confirmarReservaDeOutraClienteRetorna403() {
        String tokenOrganizador = loginAndGetToken("organizador@verzel.com", "senha123");
        UUID eventoId = criarEventoAssentosERetornarId(tokenOrganizador);
        String tokenCliente = loginAndGetToken("cliente@verzel.com", "senha123");
        UUID reservaId = criarReservaERetornarId(tokenCliente, eventoId, 2, 1);
        String tokenOutraCliente = loginAndGetToken("outra-cliente@verzel.com", "senha123");

        webTestClient.post().uri("/reservas/{id}/confirmar", reservaId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenOutraCliente)
                .bodyValue(new ConfirmarReservaRequest(FakePagamentoGatewayConfig.PAYMENT_METHOD_SUCESSO))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void confirmarReservaVencidaRetorna410() throws InterruptedException {
        String tokenOrganizador = loginAndGetToken("organizador@verzel.com", "senha123");
        UUID eventoId = criarEventoAssentosERetornarId(tokenOrganizador);
        String tokenCliente = loginAndGetToken("cliente@verzel.com", "senha123");
        UUID reservaId = criarReservaERetornarId(tokenCliente, eventoId, 2, 2);

        Thread.sleep(3200);

        webTestClient.post().uri("/reservas/{id}/confirmar", reservaId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenCliente)
                .bodyValue(new ConfirmarReservaRequest(FakePagamentoGatewayConfig.PAYMENT_METHOD_SUCESSO))
                .exchange()
                .expectStatus().isEqualTo(410);
    }

    @Test
    void cancelarReservaRetorna204ELiberaOAssentoParaNovaReserva() {
        String tokenOrganizador = loginAndGetToken("organizador@verzel.com", "senha123");
        UUID eventoId = criarEventoAssentosERetornarId(tokenOrganizador);
        String tokenCliente = loginAndGetToken("cliente@verzel.com", "senha123");
        UUID reservaId = criarReservaERetornarId(tokenCliente, eventoId, 4, 4);

        webTestClient.post().uri("/reservas/{id}/cancelar", reservaId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenCliente)
                .exchange()
                .expectStatus().isNoContent();

        String tokenOutraCliente = loginAndGetToken("outra-cliente@verzel.com", "senha123");
        webTestClient.post().uri("/eventos/{id}/reservas", eventoId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenOutraCliente)
                .bodyValue(new ReservaRequest(List.of(new AssentoRequest(4, 4)), null))
                .exchange()
                .expectStatus().isCreated();
    }

    @Test
    void cancelarReservaDeOutraClienteRetorna403() {
        String tokenOrganizador = loginAndGetToken("organizador@verzel.com", "senha123");
        UUID eventoId = criarEventoAssentosERetornarId(tokenOrganizador);
        String tokenCliente = loginAndGetToken("cliente@verzel.com", "senha123");
        UUID reservaId = criarReservaERetornarId(tokenCliente, eventoId, 5, 1);
        String tokenOutraCliente = loginAndGetToken("outra-cliente@verzel.com", "senha123");

        webTestClient.post().uri("/reservas/{id}/cancelar", reservaId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenOutraCliente)
                .exchange()
                .expectStatus().isForbidden();
    }

    private UUID criarEventoAssentosERetornarId(String tokenOrganizador) {
        EventoRequest request = new EventoRequest("Show de Rock", CategoriaEvento.SHOW, "Um grande show",
                "Arena Central", OffsetDateTime.now().plusDays(30), FormaVenda.ASSENTOS, 5, 5, null,
                new BigDecimal("120.00"));
        return webTestClient.post().uri("/eventos")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenOrganizador)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(EventoResponse.class)
                .returnResult()
                .getResponseBody()
                .id();
    }

    private UUID criarEventoPistaERetornarId(String tokenOrganizador, int quantidadeTotal) {
        EventoRequest request = new EventoRequest("Show de Pista", CategoriaEvento.SHOW, "Um grande show",
                "Arena Central", OffsetDateTime.now().plusDays(30), FormaVenda.PISTA, null, null, quantidadeTotal,
                new BigDecimal("60.00"));
        return webTestClient.post().uri("/eventos")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenOrganizador)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(EventoResponse.class)
                .returnResult()
                .getResponseBody()
                .id();
    }

    private UUID criarReservaERetornarId(String tokenCliente, UUID eventoId, int fileira, int coluna) {
        return webTestClient.post().uri("/eventos/{id}/reservas", eventoId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenCliente)
                .bodyValue(new ReservaRequest(List.of(new AssentoRequest(fileira, coluna)), null))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ReservaResponse.class)
                .returnResult()
                .getResponseBody()
                .reservaId();
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
