package com.verzel.challengebackend.security;

import com.verzel.challengebackend.repository.RevokedTokenRepository;
import java.util.List;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class JwtReactiveAuthenticationManager implements ReactiveAuthenticationManager {

    private final JwtService jwtService;
    private final RevokedTokenRepository revokedTokenRepository;

    public JwtReactiveAuthenticationManager(JwtService jwtService, RevokedTokenRepository revokedTokenRepository) {
        this.jwtService = jwtService;
        this.revokedTokenRepository = revokedTokenRepository;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String token = (String) authentication.getCredentials();
        JwtService.ParsedToken parsed;
        try {
            parsed = jwtService.parse(token);
        } catch (InvalidTokenException ex) {
            return Mono.error(new BadCredentialsException("Token inválido ou expirado", ex));
        }
        return revokedTokenRepository.existsById(parsed.jti())
                .flatMap(revoked -> {
                    if (Boolean.TRUE.equals(revoked)) {
                        return Mono.error(new BadCredentialsException("Token revogado"));
                    }
                    List<SimpleGrantedAuthority> authorities = parsed.roles().stream()
                            .map(SimpleGrantedAuthority::new)
                            .toList();
                    return Mono.just(new JwtAuthenticationToken(parsed, authorities));
                });
    }
}
