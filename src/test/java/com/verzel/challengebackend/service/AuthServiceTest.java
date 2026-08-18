package com.verzel.challengebackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.verzel.challengebackend.domain.TipoAcesso;
import com.verzel.challengebackend.domain.User;
import com.verzel.challengebackend.repository.RevokedTokenRepository;
import com.verzel.challengebackend.repository.UserRepository;
import com.verzel.challengebackend.security.JwtService;
import com.verzel.challengebackend.service.exception.InvalidCredentialsException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class AuthServiceTest {

    private UserRepository userRepository;
    private RevokedTokenRepository revokedTokenRepository;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        revokedTokenRepository = mock(RevokedTokenRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtService = mock(JwtService.class);
        authService = new AuthService(userRepository, revokedTokenRepository, passwordEncoder, jwtService);
    }

    @Test
    void loginReturnsTokenWhenCredentialsAreValid() {
        User user = new User(UUID.randomUUID(), "Ana", "Organizadora", TipoAcesso.ORGANIZADOR,
                "organizador@verzel.com", "hashed", OffsetDateTime.now());
        when(userRepository.findByEmail("organizador@verzel.com")).thenReturn(Mono.just(user));
        when(passwordEncoder.matches("senha123", "hashed")).thenReturn(true);
        when(jwtService.generateToken(user.getId(), user.getEmail(), user.getTipoAcesso())).thenReturn("jwt-token");

        StepVerifier.create(authService.login("organizador@verzel.com", "senha123"))
                .assertNext(result -> {
                    assertThat(result.token()).isEqualTo("jwt-token");
                    assertThat(result.tipoAcesso()).isEqualTo(TipoAcesso.ORGANIZADOR);
                })
                .verifyComplete();
    }

    @Test
    void loginFailsWithGenericErrorWhenPasswordIsWrong() {
        User user = new User(UUID.randomUUID(), "Ana", "Organizadora", TipoAcesso.ORGANIZADOR,
                "organizador@verzel.com", "hashed", OffsetDateTime.now());
        when(userRepository.findByEmail("organizador@verzel.com")).thenReturn(Mono.just(user));
        when(passwordEncoder.matches("senha-errada", "hashed")).thenReturn(false);

        StepVerifier.create(authService.login("organizador@verzel.com", "senha-errada"))
                .expectError(InvalidCredentialsException.class)
                .verify();
    }

    @Test
    void loginFailsWithGenericErrorWhenUserDoesNotExist() {
        when(userRepository.findByEmail("desconhecido@verzel.com")).thenReturn(Mono.empty());

        StepVerifier.create(authService.login("desconhecido@verzel.com", "qualquer"))
                .expectError(InvalidCredentialsException.class)
                .verify();
    }

    @Test
    void logoutSavesTheTokenAsRevoked() {
        UUID jti = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plusSeconds(3600);
        when(revokedTokenRepository.existsById(jti)).thenReturn(Mono.just(false));
        when(revokedTokenRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(authService.logout(jti, userId, expiresAt)).verifyComplete();

        org.mockito.Mockito.verify(revokedTokenRepository).save(org.mockito.ArgumentMatchers.argThat(
                revokedToken -> revokedToken.getJti().equals(jti) && revokedToken.getUserId().equals(userId)));
    }

    @Test
    void logoutIsIdempotentWhenTokenAlreadyRevoked() {
        UUID jti = UUID.randomUUID();
        when(revokedTokenRepository.existsById(jti)).thenReturn(Mono.just(true));

        StepVerifier.create(authService.logout(jti, UUID.randomUUID(), Instant.now().plusSeconds(3600)))
                .verifyComplete();

        org.mockito.Mockito.verify(revokedTokenRepository, org.mockito.Mockito.never())
                .save(org.mockito.ArgumentMatchers.any());
    }
}
