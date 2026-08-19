package com.verzel.challengebackend.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.verzel.challengebackend.domain.CategoriaEvento;
import com.verzel.challengebackend.domain.FormaVenda;
import com.verzel.challengebackend.domain.StatusIngresso;
import com.verzel.challengebackend.repository.IngressoRepository;
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
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

/** Exercita o StripePagamentoGateway de verdade contra a API de teste do Stripe (não o
 * FakePagamentoGatewayConfig). Requer STRIPE_SECRET_KEY setada no ambiente com uma chave
 * sk_test_... real — ver docs/stripe-test-simulation.md. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Import(TestcontainersConfig.class)
class StripeGatewayIT {

    private static final String PAYMENT_METHOD_SUCESSO = "pm_card_visa";
    private static final String PAYMENT_METHOD_RECUSADO = "pm_card_chargeDeclined";

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private IngressoRepository ingressoRepository;

    @Test
    void confirmarComPagamentoAprovadoNoStripeDeTesteRetorna200EIngressoVendido() {
        String tokenOrganizador = loginAndGetToken("organizador@verzel.com", "senha123");
        UUID eventoId = criarEventoAssentosERetornarId(tokenOrganizador);
        String tokenCliente = loginAndGetToken("cliente@verzel.com", "senha123");
        UUID reservaId = criarReservaERetornarId(tokenCliente, eventoId, 1, 1);

        webTestClient.post().uri("/reservas/{id}/confirmar", reservaId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenCliente)
                .bodyValue(new ConfirmarReservaRequest(PAYMENT_METHOD_SUCESSO))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].status").isEqualTo("VENDIDO");

        StepVerifier.create(ingressoRepository.findByReservaId(reservaId))
                .assertNext(ingresso -> {
                    assertThat(ingresso.getStatus()).isEqualTo(StatusIngresso.VENDIDO);
                    assertThat(ingresso.getStripePaymentIntentId()).startsWith("pi_");
                })
                .verifyComplete();
    }

    @Test
    void confirmarComPagamentoRecusadoNoStripeDeTesteRetorna402() {
        String tokenOrganizador = loginAndGetToken("organizador@verzel.com", "senha123");
        UUID eventoId = criarEventoAssentosERetornarId(tokenOrganizador);
        String tokenCliente = loginAndGetToken("cliente@verzel.com", "senha123");
        UUID reservaId = criarReservaERetornarId(tokenCliente, eventoId, 1, 2);

        webTestClient.post().uri("/reservas/{id}/confirmar", reservaId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenCliente)
                .bodyValue(new ConfirmarReservaRequest(PAYMENT_METHOD_RECUSADO))
                .exchange()
                .expectStatus().isEqualTo(402);

        StepVerifier.create(ingressoRepository.findByReservaId(reservaId))
                .assertNext(ingresso -> assertThat(ingresso.getStatus()).isEqualTo(StatusIngresso.RESERVADO))
                .verifyComplete();
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
