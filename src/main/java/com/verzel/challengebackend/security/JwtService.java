package com.verzel.challengebackend.security;

import com.verzel.challengebackend.domain.TipoAcesso;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtService {

    private final SecretKey signingKey;
    private final Duration expiration;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-seconds}") long expirationSeconds) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = Duration.ofSeconds(expirationSeconds);
    }

    public String generateToken(UUID userId, String email, TipoAcesso tipoAcesso) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(expiration);
        return Jwts.builder()
                .subject(userId.toString())
                .id(UUID.randomUUID().toString())
                .claim("email", email)
                .claim("tipoAcesso", tipoAcesso.name())
                .claim("roles", List.of("ROLE_" + tipoAcesso.name()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }

    @SuppressWarnings("unchecked")
    public ParsedToken parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            List<String> roles = (List<String>) claims.get("roles", List.class);
            return new ParsedToken(
                    UUID.fromString(claims.getSubject()),
                    UUID.fromString(claims.getId()),
                    claims.get("email", String.class),
                    TipoAcesso.valueOf(claims.get("tipoAcesso", String.class)),
                    roles,
                    claims.getExpiration().toInstant());
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidTokenException("Token inválido ou expirado", ex);
        }
    }

    public record ParsedToken(
            UUID userId, UUID jti, String email, TipoAcesso tipoAcesso, List<String> roles, Instant expiresAt) {
    }
}
