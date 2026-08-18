package com.verzel.challengebackend.service;

import com.verzel.challengebackend.domain.RevokedToken;
import com.verzel.challengebackend.domain.TipoAcesso;
import com.verzel.challengebackend.domain.User;
import com.verzel.challengebackend.repository.RevokedTokenRepository;
import com.verzel.challengebackend.repository.UserRepository;
import com.verzel.challengebackend.security.JwtService;
import com.verzel.challengebackend.service.exception.InvalidCredentialsException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RevokedTokenRepository revokedTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            RevokedTokenRepository revokedTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.revokedTokenRepository = revokedTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public Mono<LoginResult> login(String email, String senha) {
        return userRepository.findByEmail(email)
                .filter(user -> passwordEncoder.matches(senha, user.getSenha()))
                .switchIfEmpty(Mono.error(new InvalidCredentialsException()))
                .map(user -> new LoginResult(
                        jwtService.generateToken(user.getId(), user.getEmail(), user.getTipoAcesso()),
                        user.getTipoAcesso()));
    }

    public Mono<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    public Mono<Void> logout(UUID jti, UUID userId, Instant expiresAt) {
        return revokedTokenRepository.existsById(jti)
                .flatMap(alreadyRevoked -> {
                    if (Boolean.TRUE.equals(alreadyRevoked)) {
                        return Mono.empty();
                    }
                    RevokedToken revokedToken = new RevokedToken(
                            jti, userId, OffsetDateTime.ofInstant(expiresAt, ZoneOffset.UTC), OffsetDateTime.now());
                    return revokedTokenRepository.save(revokedToken);
                })
                .then();
    }

    public record LoginResult(String token, TipoAcesso tipoAcesso) {
    }
}
