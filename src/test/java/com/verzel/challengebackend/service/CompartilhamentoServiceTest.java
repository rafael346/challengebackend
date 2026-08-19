package com.verzel.challengebackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.verzel.challengebackend.domain.CategoriaEvento;
import com.verzel.challengebackend.domain.Evento;
import com.verzel.challengebackend.domain.FormaVenda;
import com.verzel.challengebackend.domain.Ingresso;
import com.verzel.challengebackend.domain.StatusIngresso;
import com.verzel.challengebackend.repository.EventoRepository;
import com.verzel.challengebackend.repository.IngressoRepository;
import com.verzel.challengebackend.service.exception.IngressoNotFoundException;
import com.verzel.challengebackend.service.exception.InvalidReservaException;
import com.verzel.challengebackend.service.exception.ReservaAccessDeniedException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class CompartilhamentoServiceTest {

    private final IngressoRepository ingressoRepository = mock(IngressoRepository.class);
    private final EventoRepository eventoRepository = mock(EventoRepository.class);
    private final CompartilhamentoService compartilhamentoService =
            new CompartilhamentoService(ingressoRepository, eventoRepository);

    @Test
    void compartilharIngressoInexistenteLancaIngressoNotFoundException() {
        UUID ingressoId = UUID.randomUUID();
        when(ingressoRepository.findById(ingressoId)).thenReturn(Mono.empty());

        StepVerifier.create(compartilhamentoService.compartilhar(ingressoId, UUID.randomUUID()))
                .expectError(IngressoNotFoundException.class)
                .verify();
    }

    @Test
    void compartilharIngressoDeOutroCompradorLancaReservaAccessDeniedException() {
        UUID compradorId = UUID.randomUUID();
        Ingresso ingresso = ingressoVendido(UUID.randomUUID(), null);
        when(ingressoRepository.findById(ingresso.getId())).thenReturn(Mono.just(ingresso));

        StepVerifier.create(compartilhamentoService.compartilhar(ingresso.getId(), compradorId))
                .expectError(ReservaAccessDeniedException.class)
                .verify();
    }

    @Test
    void compartilharIngressoNaoVendidoLancaInvalidReservaException() {
        UUID compradorId = UUID.randomUUID();
        OffsetDateTime agora = OffsetDateTime.now();
        Ingresso reservado = new Ingresso(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), compradorId, 1, 1,
                new BigDecimal("100.00"), StatusIngresso.RESERVADO, agora.plusMinutes(10), agora.plusHours(200),
                null, agora, agora);
        when(ingressoRepository.findById(reservado.getId())).thenReturn(Mono.just(reservado));

        StepVerifier.create(compartilhamentoService.compartilhar(reservado.getId(), compradorId))
                .expectError(InvalidReservaException.class)
                .verify();
    }

    @Test
    void compartilharIngressoSemTokenGeraNovoTokenESalva() {
        UUID compradorId = UUID.randomUUID();
        Ingresso ingresso = ingressoVendido(compradorId, null);
        when(ingressoRepository.findById(ingresso.getId())).thenReturn(Mono.just(ingresso));
        when(ingressoRepository.save(any(Ingresso.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(compartilhamentoService.compartilhar(ingresso.getId(), compradorId))
                .assertNext(token -> assertThat(token).isNotNull())
                .verifyComplete();
        verify(ingressoRepository).save(any(Ingresso.class));
    }

    @Test
    void compartilharIngressoComTokenExistenteEIdempotenteENaoSalvaDeNovo() {
        UUID compradorId = UUID.randomUUID();
        UUID tokenExistente = UUID.randomUUID();
        Ingresso ingresso = ingressoVendido(compradorId, tokenExistente);
        when(ingressoRepository.findById(ingresso.getId())).thenReturn(Mono.just(ingresso));

        StepVerifier.create(compartilhamentoService.compartilhar(ingresso.getId(), compradorId))
                .assertNext(token -> assertThat(token).isEqualTo(tokenExistente))
                .verifyComplete();
        verify(ingressoRepository, never()).save(any(Ingresso.class));
    }

    @Test
    void buscarPorTokenInexistenteLancaIngressoNotFoundException() {
        UUID token = UUID.randomUUID();
        when(ingressoRepository.findByCompartilhamentoToken(token)).thenReturn(Mono.empty());

        StepVerifier.create(compartilhamentoService.buscarPorToken(token))
                .expectError(IngressoNotFoundException.class)
                .verify();
    }

    @Test
    void buscarPorTokenExistenteRetornaIngressoEEvento() {
        UUID token = UUID.randomUUID();
        UUID eventoId = UUID.randomUUID();
        Ingresso ingresso = ingressoVendido(UUID.randomUUID(), token, eventoId);
        Evento evento = evento(eventoId);
        when(ingressoRepository.findByCompartilhamentoToken(token)).thenReturn(Mono.just(ingresso));
        when(eventoRepository.findById(eventoId)).thenReturn(Mono.just(evento));

        StepVerifier.create(compartilhamentoService.buscarPorToken(token))
                .assertNext(compartilhado -> {
                    assertThat(compartilhado.ingresso().getId()).isEqualTo(ingresso.getId());
                    assertThat(compartilhado.evento().getTitulo()).isEqualTo("Show de Teste");
                })
                .verifyComplete();
    }

    private Ingresso ingressoVendido(UUID compradorId, UUID compartilhamentoToken) {
        return ingressoVendido(compradorId, compartilhamentoToken, UUID.randomUUID());
    }

    private Ingresso ingressoVendido(UUID compradorId, UUID compartilhamentoToken, UUID eventoId) {
        OffsetDateTime agora = OffsetDateTime.now();
        return new Ingresso(UUID.randomUUID(), eventoId, UUID.randomUUID(), compradorId, 1, 1,
                new BigDecimal("100.00"), StatusIngresso.VENDIDO, agora.minusMinutes(20), agora.plusHours(200),
                "pi_123", agora, agora, null, null, compartilhamentoToken);
    }

    private Evento evento(UUID id) {
        OffsetDateTime agora = OffsetDateTime.now();
        return new Evento(id, "Show de Teste", CategoriaEvento.SHOW, "Descrição", "Arena", agora.plusDays(10),
                FormaVenda.ASSENTOS, 10, 10, 100, new BigDecimal("100.00"), UUID.randomUUID(), agora, agora);
    }
}
