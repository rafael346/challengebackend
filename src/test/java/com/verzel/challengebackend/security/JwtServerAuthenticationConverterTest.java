package com.verzel.challengebackend.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

class JwtServerAuthenticationConverterTest {

    private final JwtServerAuthenticationConverter converter = new JwtServerAuthenticationConverter();

    @Test
    void extractsTokenFromBearerAuthorizationHeader() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer abc.def.ghi"));

        StepVerifier.create(converter.convert(exchange))
                .assertNext(auth -> assertThat(auth.getCredentials()).isEqualTo("abc.def.ghi"))
                .verifyComplete();
    }

    @Test
    void returnsEmptyWhenAuthorizationHeaderIsMissing() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/auth/me"));

        StepVerifier.create(converter.convert(exchange)).verifyComplete();
    }

    @Test
    void returnsEmptyWhenAuthorizationHeaderDoesNotUseBearerScheme() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/auth/me").header(HttpHeaders.AUTHORIZATION, "Basic abc123"));

        StepVerifier.create(converter.convert(exchange)).verifyComplete();
    }
}
