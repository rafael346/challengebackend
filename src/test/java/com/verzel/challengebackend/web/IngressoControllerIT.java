package com.verzel.challengebackend.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.verzel.challengebackend.domain.CategoriaEvento;
import com.verzel.challengebackend.domain.FormaVenda;
import com.verzel.challengebackend.support.FakePagamentoGatewayConfig;
import com.verzel.challengebackend.support.TestcontainersConfig;
import com.verzel.challengebackend.web.dto.AssentoRequest;
import com.verzel.challengebackend.web.dto.CompartilhamentoResponse;
import com.verzel.challengebackend.web.dto.ConfirmarReservaRequest;
import com.verzel.challengebackend.web.dto.EventoRequest;
import com.verzel.challengebackend.web.dto.EventoResponse;
import com.verzel.challengebackend.web.dto.IngressoResponse;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Import({TestcontainersConfig.class, FakePagamentoGatewayConfig.class})
class IngressoControllerIT {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void validarIngressoVendidoComPortariaRetornaValido() {
        String tokenPortaria = loginAndGetToken("portaria@verzel.com", "senha123");
        UUID eventoId = criarEventoERetornarId();
        UUID ingressoId = criarEComprarIngressoERetornarId(eventoId, 1, 1);

        webTestClient.post().uri("/eventos/{eventoId}/ingressos/{ingressoId}/validar", eventoId, ingressoId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenPortaria)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resultado").isEqualTo("VALIDO")
                .jsonPath("$.ingressoId").isEqualTo(ingressoId.toString());
    }

    @Test
    void validarOMesmoIngressoDuasVezesRetornaJaUtilizadoNaSegunda() {
        String tokenPortaria = loginAndGetToken("portaria@verzel.com", "senha123");
        UUID eventoId = criarEventoERetornarId();
        UUID ingressoId = criarEComprarIngressoERetornarId(eventoId, 1, 2);

        webTestClient.post().uri("/eventos/{eventoId}/ingressos/{ingressoId}/validar", eventoId, ingressoId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenPortaria)
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.resultado").isEqualTo("VALIDO");

        webTestClient.post().uri("/eventos/{eventoId}/ingressos/{ingressoId}/validar", eventoId, ingressoId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenPortaria)
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.resultado").isEqualTo("JA_UTILIZADO");
    }

    @Test
    void validarComEventoErradoRetornaEventoErrado() {
        String tokenPortaria = loginAndGetToken("portaria@verzel.com", "senha123");
        UUID eventoId = criarEventoERetornarId();
        UUID outroEventoId = criarEventoERetornarId();
        UUID ingressoId = criarEComprarIngressoERetornarId(eventoId, 1, 3);

        webTestClient.post().uri("/eventos/{eventoId}/ingressos/{ingressoId}/validar", outroEventoId, ingressoId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenPortaria)
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.resultado").isEqualTo("EVENTO_ERRADO");
    }

    @Test
    void validarComTokenDeClienteRetorna403() {
        String tokenCliente = loginAndGetToken("cliente@verzel.com", "senha123");
        UUID eventoId = criarEventoERetornarId();
        UUID ingressoId = criarEComprarIngressoERetornarId(eventoId, 1, 4);

        webTestClient.post().uri("/eventos/{eventoId}/ingressos/{ingressoId}/validar", eventoId, ingressoId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenCliente)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void validarIngressoInexistenteRetorna404() {
        String tokenPortaria = loginAndGetToken("portaria@verzel.com", "senha123");
        UUID eventoId = criarEventoERetornarId();

        webTestClient.post().uri("/eventos/{eventoId}/ingressos/{ingressoId}/validar", eventoId, UUID.randomUUID())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenPortaria)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void compartilharIngressoVendidoRetornaLinkComTokenDeCompartilhamento() {
        UUID eventoId = criarEventoERetornarId();
        UUID ingressoId = criarEComprarIngressoERetornarId(eventoId, 2, 1);
        String tokenCliente = loginAndGetToken("cliente@verzel.com", "senha123");

        CompartilhamentoResponse resposta = webTestClient.post().uri("/ingressos/{id}/compartilhar", ingressoId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenCliente)
                .exchange()
                .expectStatus().isOk()
                .expectBody(CompartilhamentoResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(resposta.linkPublico()).contains("/ingressos/compartilhados/");
    }

    @Test
    void compartilharIngressoDeOutraClienteRetorna403() {
        UUID eventoId = criarEventoERetornarId();
        UUID ingressoId = criarEComprarIngressoERetornarId(eventoId, 2, 2);
        String tokenOutraCliente = loginAndGetToken("outra-cliente@verzel.com", "senha123");

        webTestClient.post().uri("/ingressos/{id}/compartilhar", ingressoId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenOutraCliente)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void buscarIngressoCompartilhadoSemAutenticacaoRetorna200() {
        UUID eventoId = criarEventoERetornarId();
        UUID ingressoId = criarEComprarIngressoERetornarId(eventoId, 2, 3);
        String tokenCliente = loginAndGetToken("cliente@verzel.com", "senha123");
        String linkPublico = webTestClient.post().uri("/ingressos/{id}/compartilhar", ingressoId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenCliente)
                .exchange()
                .expectStatus().isOk()
                .expectBody(CompartilhamentoResponse.class)
                .returnResult()
                .getResponseBody()
                .linkPublico();
        String path = linkPublico.substring(linkPublico.indexOf("/ingressos/compartilhados/"));

        webTestClient.get().uri(path)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.ingressoId").isEqualTo(ingressoId.toString())
                .jsonPath("$.status").isEqualTo("VENDIDO");
    }

    @Test
    void buscarIngressoCompartilhadoComTokenInexistenteRetorna404() {
        webTestClient.get().uri("/ingressos/compartilhados/{token}", UUID.randomUUID())
                .exchange()
                .expectStatus().isNotFound();
    }

    private UUID criarEventoERetornarId() {
        String tokenOrganizador = loginAndGetToken("organizador@verzel.com", "senha123");
        EventoRequest request = new EventoRequest("Show de Validação", CategoriaEvento.SHOW, "Um grande show",
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

    private UUID criarEComprarIngressoERetornarId(UUID eventoId, int fileira, int coluna) {
        String tokenCliente = loginAndGetToken("cliente@verzel.com", "senha123");
        UUID reservaId = webTestClient.post().uri("/eventos/{id}/reservas", eventoId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenCliente)
                .bodyValue(new ReservaRequest(List.of(new AssentoRequest(fileira, coluna)), null))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ReservaResponse.class)
                .returnResult()
                .getResponseBody()
                .reservaId();

        return webTestClient.post().uri("/reservas/{id}/confirmar", reservaId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenCliente)
                .bodyValue(new ConfirmarReservaRequest(FakePagamentoGatewayConfig.PAYMENT_METHOD_SUCESSO))
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(IngressoResponse.class)
                .returnResult()
                .getResponseBody()
                .get(0)
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
