package com.verzel.challengebackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.verzel.challengebackend.domain.Ingresso;
import com.verzel.challengebackend.domain.StatusIngresso;
import com.verzel.challengebackend.repository.IngressoRepository;
import com.verzel.challengebackend.service.exception.IngressoNotFoundException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ValidacaoServiceTest {

    private final IngressoRepository ingressoRepository = mock(IngressoRepository.class);
    private final ValidacaoService validacaoService = new ValidacaoService(ingressoRepository);

    @Test
    void validarIngressoInexistenteLancaIngressoNotFoundException() {
        UUID ingressoId = UUID.randomUUID();
        when(ingressoRepository.findById(ingressoId)).thenReturn(Mono.empty());

        StepVerifier.create(validacaoService.validar(UUID.randomUUID(), ingressoId, UUID.randomUUID()))
                .expectError(IngressoNotFoundException.class)
                .verify();
    }

    @Test
    void validarComEventoDiferenteRetornaEventoErrado() {
        UUID eventoId = UUID.randomUUID();
        Ingresso ingresso = ingressoVendido(UUID.randomUUID(), OffsetDateTime.now().plusHours(1));
        when(ingressoRepository.findById(ingresso.getId())).thenReturn(Mono.just(ingresso));

        StepVerifier.create(validacaoService.validar(eventoId, ingresso.getId(), UUID.randomUUID()))
                .assertNext(resultado -> assertThat(resultado.resultado())
                        .isEqualTo(ResultadoValidacao.EVENTO_ERRADO))
                .verifyComplete();
    }

    @Test
    void validarIngressoJaUsadoRetornaJaUtilizadoComDadosDaValidacaoAnterior() {
        UUID eventoId = UUID.randomUUID();
        UUID validadoPorId = UUID.randomUUID();
        OffsetDateTime validadoEm = OffsetDateTime.now().minusMinutes(30);
        Ingresso ingresso = ingressoVendido(eventoId, OffsetDateTime.now().plusHours(1))
                .usado(validadoPorId, validadoEm);
        when(ingressoRepository.findById(ingresso.getId())).thenReturn(Mono.just(ingresso));

        StepVerifier.create(validacaoService.validar(eventoId, ingresso.getId(), UUID.randomUUID()))
                .assertNext(resultado -> {
                    assertThat(resultado.resultado()).isEqualTo(ResultadoValidacao.JA_UTILIZADO);
                    assertThat(resultado.validadoPorId()).isEqualTo(validadoPorId);
                })
                .verifyComplete();
    }

    @Test
    void validarIngressoReservadoRetornaInvalido() {
        UUID eventoId = UUID.randomUUID();
        Ingresso ingresso = ingressoComStatus(eventoId, StatusIngresso.RESERVADO, OffsetDateTime.now().plusHours(1));
        when(ingressoRepository.findById(ingresso.getId())).thenReturn(Mono.just(ingresso));

        StepVerifier.create(validacaoService.validar(eventoId, ingresso.getId(), UUID.randomUUID()))
                .assertNext(resultado -> assertThat(resultado.resultado()).isEqualTo(ResultadoValidacao.INVALIDO))
                .verifyComplete();
    }

    @Test
    void validarIngressoVendidoAposValidoAteRetornaExpirado() {
        UUID eventoId = UUID.randomUUID();
        Ingresso ingresso = ingressoVendido(eventoId, OffsetDateTime.now().minusMinutes(1));
        when(ingressoRepository.findById(ingresso.getId())).thenReturn(Mono.just(ingresso));

        StepVerifier.create(validacaoService.validar(eventoId, ingresso.getId(), UUID.randomUUID()))
                .assertNext(resultado -> assertThat(resultado.resultado()).isEqualTo(ResultadoValidacao.EXPIRADO))
                .verifyComplete();
    }

    @Test
    void validarIngressoVendidoDentroDoPrazoMarcaComoUsadoERetornaValido() {
        UUID eventoId = UUID.randomUUID();
        UUID portariaId = UUID.randomUUID();
        Ingresso ingresso = ingressoVendido(eventoId, OffsetDateTime.now().plusHours(1));
        when(ingressoRepository.findById(ingresso.getId())).thenReturn(Mono.just(ingresso));
        when(ingressoRepository.validarUso(eq(ingresso.getId()), eq(portariaId), any(OffsetDateTime.class)))
                .thenReturn(Mono.just(1));

        StepVerifier.create(validacaoService.validar(eventoId, ingresso.getId(), portariaId))
                .assertNext(resultado -> {
                    assertThat(resultado.resultado()).isEqualTo(ResultadoValidacao.VALIDO);
                    assertThat(resultado.ingressoId()).isEqualTo(ingresso.getId());
                    assertThat(resultado.validadoPorId()).isEqualTo(portariaId);
                })
                .verifyComplete();
    }

    @Test
    void validarPerdendoACorridaDeConcorrenciaRetornaJaUtilizado() {
        UUID eventoId = UUID.randomUUID();
        UUID outraPortariaId = UUID.randomUUID();
        OffsetDateTime validadoEm = OffsetDateTime.now();
        Ingresso ingresso = ingressoVendido(eventoId, OffsetDateTime.now().plusHours(1));
        Ingresso jaUsadoPorOutraPortaria = ingresso.usado(outraPortariaId, validadoEm);
        when(ingressoRepository.findById(ingresso.getId()))
                .thenReturn(Mono.just(ingresso))
                .thenReturn(Mono.just(jaUsadoPorOutraPortaria));
        when(ingressoRepository.validarUso(eq(ingresso.getId()), any(), any(OffsetDateTime.class)))
                .thenReturn(Mono.just(0));

        StepVerifier.create(validacaoService.validar(eventoId, ingresso.getId(), UUID.randomUUID()))
                .assertNext(resultado -> {
                    assertThat(resultado.resultado()).isEqualTo(ResultadoValidacao.JA_UTILIZADO);
                    assertThat(resultado.validadoPorId()).isEqualTo(outraPortariaId);
                })
                .verifyComplete();
    }

    private Ingresso ingressoVendido(UUID eventoId, OffsetDateTime validoAte) {
        return ingressoComStatus(eventoId, StatusIngresso.VENDIDO, validoAte);
    }

    private Ingresso ingressoComStatus(UUID eventoId, StatusIngresso status, OffsetDateTime validoAte) {
        OffsetDateTime agora = OffsetDateTime.now();
        return new Ingresso(UUID.randomUUID(), eventoId, UUID.randomUUID(), UUID.randomUUID(), 1, 1,
                new BigDecimal("100.00"), status, agora.minusMinutes(20), validoAte, "pi_123", agora, agora);
    }
}
