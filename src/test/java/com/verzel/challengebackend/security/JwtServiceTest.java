package com.verzel.challengebackend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.verzel.challengebackend.domain.TipoAcesso;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "test-only-secret-key-with-at-least-32-bytes-000000";

    private final JwtService jwtService = new JwtService(SECRET, 3600);

    @Test
    void generatesTokenThatCanBeParsedBackWithSameClaims() {
        UUID userId = UUID.randomUUID();
        String email = "organizador@verzel.com";
        TipoAcesso tipoAcesso = TipoAcesso.ORGANIZADOR;

        String token = jwtService.generateToken(userId, email, tipoAcesso);
        JwtService.ParsedToken parsed = jwtService.parse(token);

        assertThat(parsed.userId()).isEqualTo(userId);
        assertThat(parsed.email()).isEqualTo(email);
        assertThat(parsed.tipoAcesso()).isEqualTo(tipoAcesso);
        assertThat(parsed.roles()).containsExactly("ROLE_ORGANIZADOR");
        assertThat(parsed.jti()).isNotNull();
    }

    @Test
    void eachGeneratedTokenHasAUniqueJti() {
        UUID userId = UUID.randomUUID();
        String token1 = jwtService.generateToken(userId, "a@verzel.com", TipoAcesso.CLIENTE);
        String token2 = jwtService.generateToken(userId, "a@verzel.com", TipoAcesso.CLIENTE);

        assertThat(jwtService.parse(token1).jti()).isNotEqualTo(jwtService.parse(token2).jti());
    }

    @Test
    void parsingAnExpiredTokenThrowsInvalidTokenException() {
        JwtService shortLivedJwtService = new JwtService(SECRET, -1);
        String expiredToken =
                shortLivedJwtService.generateToken(UUID.randomUUID(), "a@verzel.com", TipoAcesso.PORTARIA);

        assertThatThrownBy(() -> jwtService.parse(expiredToken))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void parsingATokenSignedWithADifferentSecretThrowsInvalidTokenException() {
        JwtService otherJwtService = new JwtService("another-completely-different-secret-key-000000000", 3600);
        String token = otherJwtService.generateToken(UUID.randomUUID(), "a@verzel.com", TipoAcesso.CLIENTE);

        assertThatThrownBy(() -> jwtService.parse(token))
                .isInstanceOf(InvalidTokenException.class);
    }
}
