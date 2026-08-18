package com.verzel.challengebackend.web;

import com.verzel.challengebackend.security.JwtAuthenticationToken;
import com.verzel.challengebackend.security.JwtService;
import com.verzel.challengebackend.service.AuthService;
import com.verzel.challengebackend.web.dto.LoginRequest;
import com.verzel.challengebackend.web.dto.LoginResponse;
import com.verzel.challengebackend.web.dto.LogoutResponse;
import com.verzel.challengebackend.web.dto.MeResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/auth/login")
    public Mono<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.email(), request.senha())
                .map(result -> new LoginResponse(result.token(), result.tipoAcesso()));
    }

    @GetMapping("/auth/me")
    public Mono<MeResponse> me() {
        return currentToken()
                .flatMap(parsed -> authService.findById(parsed.userId()))
                .map(user -> new MeResponse(user.getId(), user.getNome(), user.getSobrenome(), user.getEmail(),
                        user.getTipoAcesso()));
    }

    @PostMapping("/auth/logout")
    public Mono<LogoutResponse> logout() {
        return currentToken()
                .flatMap(parsed -> authService.logout(parsed.jti(), parsed.userId(), parsed.expiresAt()))
                .thenReturn(new LogoutResponse("Logout realizado com sucesso"));
    }

    private Mono<JwtService.ParsedToken> currentToken() {
        return ReactiveSecurityContextHolder.getContext()
                .map(context -> (JwtAuthenticationToken) context.getAuthentication())
                .map(auth -> (JwtService.ParsedToken) auth.getPrincipal());
    }
}
