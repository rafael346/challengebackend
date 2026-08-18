package com.verzel.challengebackend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.verzel.challengebackend.domain.TipoAcesso;
import com.verzel.challengebackend.repository.RevokedTokenRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class JwtReactiveAuthenticationManagerTest {

    private final JwtService jwtService = mock(JwtService.class);
    private final RevokedTokenRepository revokedTokenRepository = mock(RevokedTokenRepository.class);
    private final JwtReactiveAuthenticationManager manager =
            new JwtReactiveAuthenticationManager(jwtService, revokedTokenRepository);

    @Test
    void authenticatesWhenTokenIsValidAndNotRevoked() {
        UUID jti = UUID.randomUUID();
        var parsed = new JwtService.ParsedToken(
                UUID.randomUUID(), jti, "a@verzel.com", TipoAcesso.ORGANIZADOR,
                List.of("ROLE_ORGANIZADOR"), Instant.now().plusSeconds(3600));
        when(jwtService.parse("valid-token")).thenReturn(parsed);
        when(revokedTokenRepository.existsById(jti)).thenReturn(Mono.just(false));

        StepVerifier.create(manager.authenticate(new JwtAuthenticationToken("valid-token")))
                .assertNext(auth -> {
                    assertThat(auth.isAuthenticated()).isTrue();
                    assertThat(auth.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_ORGANIZADOR");
                })
                .verifyComplete();
    }

    @Test
    void rejectsATokenThatWasRevoked() {
        UUID jti = UUID.randomUUID();
        var parsed = new JwtService.ParsedToken(
                UUID.randomUUID(), jti, "a@verzel.com", TipoAcesso.ORGANIZADOR,
                List.of("ROLE_ORGANIZADOR"), Instant.now().plusSeconds(3600));
        when(jwtService.parse("revoked-token")).thenReturn(parsed);
        when(revokedTokenRepository.existsById(jti)).thenReturn(Mono.just(true));

        StepVerifier.create(manager.authenticate(new JwtAuthenticationToken("revoked-token")))
                .expectError(BadCredentialsException.class)
                .verify();
    }

    @Test
    void rejectsAMalformedOrExpiredToken() {
        when(jwtService.parse("bad-token")).thenThrow(new InvalidTokenException("invalid", null));

        StepVerifier.create(manager.authenticate(new JwtAuthenticationToken("bad-token")))
                .expectError(BadCredentialsException.class)
                .verify();
    }
}
