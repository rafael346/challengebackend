package com.verzel.challengebackend.web;

import com.verzel.challengebackend.support.TestcontainersConfig;
import com.verzel.challengebackend.web.dto.LoginRequest;
import com.verzel.challengebackend.web.dto.LoginResponse;
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
class AuthControllerIT {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void loginWithValidCredentialsReturnsTokenAndTipoAcesso() {
        webTestClient.post().uri("/auth/login")
                .bodyValue(new LoginRequest("organizador@verzel.com", "senha123"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.token").isNotEmpty()
                .jsonPath("$.tipoAcesso").isEqualTo("ORGANIZADOR");
    }

    @Test
    void loginWithWrongPasswordReturns401WithGenericMessage() {
        webTestClient.post().uri("/auth/login")
                .bodyValue(new LoginRequest("organizador@verzel.com", "senha-errada"))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Email ou senha inválidos");
    }

    @Test
    void loginWithUnknownEmailReturns401WithSameGenericMessage() {
        webTestClient.post().uri("/auth/login")
                .bodyValue(new LoginRequest("nao-existe@verzel.com", "qualquer"))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Email ou senha inválidos");
    }

    @Test
    void loginWithInvalidEmailFormatReturns400() {
        webTestClient.post().uri("/auth/login")
                .bodyValue(new LoginRequest("nao-e-um-email", "senha123"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Dados inválidos")
                .jsonPath("$.errors.email").isNotEmpty();
    }

    @Test
    void meReturnsAuthenticatedUserDataGivenAValidToken() {
        String token = loginAndGetToken("cliente@verzel.com", "senha123");

        webTestClient.get().uri("/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.email").isEqualTo("cliente@verzel.com")
                .jsonPath("$.tipoAcesso").isEqualTo("CLIENTE");
    }

    @Test
    void meWithoutTokenReturns401() {
        webTestClient.get().uri("/auth/me")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void meWithInvalidTokenReturns401() {
        webTestClient.get().uri("/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer token-invalido")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void logoutRevokesTheCurrentTokenAndSubsequentUseIsRejected() {
        String token = loginAndGetToken("portaria@verzel.com", "senha123");

        webTestClient.post().uri("/auth/logout")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Logout realizado com sucesso");

        webTestClient.get().uri("/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void logoutWithoutTokenReturns401() {
        webTestClient.post().uri("/auth/logout")
                .exchange()
                .expectStatus().isUnauthorized();
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
