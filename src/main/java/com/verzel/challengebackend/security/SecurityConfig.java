package com.verzel.challengebackend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.web.cors.reactive.CorsConfigurationSource;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            ReactiveAuthenticationManager authenticationManager,
            JwtServerAuthenticationConverter authenticationConverter,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
            CorsConfigurationSource corsConfigurationSource) {

        AuthenticationWebFilter jwtAuthenticationFilter = new AuthenticationWebFilter(authenticationManager);
        jwtAuthenticationFilter.setServerAuthenticationConverter(authenticationConverter);

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAuthenticationEntryPoint))
                .authorizeExchange(exchange -> exchange
                        .pathMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .pathMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        .pathMatchers(HttpMethod.GET, "/eventos", "/eventos/**").permitAll()
                        .pathMatchers(HttpMethod.POST, "/eventos").hasRole("ORGANIZADOR")
                        .pathMatchers(HttpMethod.PUT, "/eventos/**").hasRole("ORGANIZADOR")
                        .pathMatchers(HttpMethod.DELETE, "/eventos/**").hasRole("ORGANIZADOR")
                        .pathMatchers(HttpMethod.POST, "/eventos/*/reservas").hasRole("CLIENTE")
                        .pathMatchers(HttpMethod.POST, "/reservas/**").hasRole("CLIENTE")
                        .pathMatchers(HttpMethod.POST, "/eventos/*/ingressos/*/validar").hasRole("PORTARIA")
                        .pathMatchers(HttpMethod.GET, "/ingressos/compartilhados/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/ingressos").hasRole("CLIENTE")
                        .pathMatchers(HttpMethod.POST, "/ingressos/*/compartilhar").hasRole("CLIENTE")
                        .anyExchange().authenticated())
                .addFilterAt(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }
}
