package com.verzel.challengebackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.verzel.challengebackend.service.exception.AssentoIndisponivelException;
import com.verzel.challengebackend.service.exception.EventoNotFoundException;
import com.verzel.challengebackend.service.exception.InvalidReservaException;
import com.verzel.challengebackend.service.exception.QuantidadeIndisponivelException;
import com.verzel.challengebackend.web.dto.AssentoRequest;
import com.verzel.challengebackend.web.dto.ReservaRequest;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ReservaServiceTest {

    private EventoRepository eventoRepository;
    private IngressoRepository ingressoRepository;
    private ReservaService reservaService;

    @BeforeEach
    void setUp() {
        eventoRepository = mock(EventoRepository.class);
        ingressoRepository = mock(IngressoRepository.class);
        reservaService = new ReservaService(eventoRepository, ingressoRepository, 600);
    }

    @Test
    void criarReservaDeAssentosLivresInsereUmIngressoPorAssento() {
        UUID eventoId = UUID.randomUUID();
        UUID compradorId = UUID.randomUUID();
        Evento evento = eventoAssentos(eventoId);
        ReservaRequest request = new ReservaRequest(List.of(new AssentoRequest(1, 1), new AssentoRequest(1, 2)), null);
        when(eventoRepository.buscarComLockPorId(eventoId)).thenReturn(Mono.just(evento));
        when(ingressoRepository.expirarReservasVencidas(eventoId)).thenReturn(Mono.just(0));
        when(ingressoRepository.buscarAssentoAtivo(eq(eventoId), any(), any())).thenReturn(Mono.empty());
        when(ingressoRepository.saveAll(any(List.class)))
                .thenAnswer(invocation -> Flux.fromIterable((List<Ingresso>) invocation.getArgument(0)));

        StepVerifier.create(reservaService.criar(eventoId, request, compradorId))
                .assertNext(ingressos -> {
                    assertThat(ingressos).hasSize(2);
                    assertThat(ingressos).allMatch(i -> i.getStatus() == StatusIngresso.RESERVADO);
                    assertThat(ingressos).allMatch(i -> i.getCompradorId().equals(compradorId));
                    assertThat(ingressos.get(0).getReservaId()).isEqualTo(ingressos.get(1).getReservaId());
                    assertThat(ingressos.get(0).getPreco()).isEqualByComparingTo("100.00");
                })
                .verifyComplete();
    }

    @Test
    void criarReservaDeAssentoJaOcupadoLancaAssentoIndisponivelException() {
        UUID eventoId = UUID.randomUUID();
        Evento evento = eventoAssentos(eventoId);
        ReservaRequest request = new ReservaRequest(List.of(new AssentoRequest(1, 1)), null);
        Ingresso ocupado = mock(Ingresso.class);
        when(eventoRepository.buscarComLockPorId(eventoId)).thenReturn(Mono.just(evento));
        when(ingressoRepository.expirarReservasVencidas(eventoId)).thenReturn(Mono.just(0));
        when(ingressoRepository.buscarAssentoAtivo(eventoId, 1, 1)).thenReturn(Mono.just(ocupado));

        StepVerifier.create(reservaService.criar(eventoId, request, UUID.randomUUID()))
                .expectError(AssentoIndisponivelException.class)
                .verify();
        verify(ingressoRepository, never()).saveAll(any(List.class));
    }

    @Test
    void criarReservaComAssentoForaDoGridLancaInvalidReservaException() {
        UUID eventoId = UUID.randomUUID();
        Evento evento = eventoAssentos(eventoId);
        ReservaRequest request = new ReservaRequest(List.of(new AssentoRequest(99, 99)), null);
        when(eventoRepository.buscarComLockPorId(eventoId)).thenReturn(Mono.just(evento));
        when(ingressoRepository.expirarReservasVencidas(eventoId)).thenReturn(Mono.just(0));

        StepVerifier.create(reservaService.criar(eventoId, request, UUID.randomUUID()))
                .expectError(InvalidReservaException.class)
                .verify();
    }

    @Test
    void criarReservaDePistaDentroDaCapacidadeInsereAQuantidadePedida() {
        UUID eventoId = UUID.randomUUID();
        UUID compradorId = UUID.randomUUID();
        Evento evento = eventoPista(eventoId, 10);
        ReservaRequest request = new ReservaRequest(null, 3);
        when(eventoRepository.buscarComLockPorId(eventoId)).thenReturn(Mono.just(evento));
        when(ingressoRepository.expirarReservasVencidas(eventoId)).thenReturn(Mono.just(0));
        when(ingressoRepository.contarAtivosPorEvento(eventoId)).thenReturn(Mono.just(0L));
        when(ingressoRepository.saveAll(any(List.class)))
                .thenAnswer(invocation -> Flux.fromIterable((List<Ingresso>) invocation.getArgument(0)));

        StepVerifier.create(reservaService.criar(eventoId, request, compradorId))
                .assertNext(ingressos -> {
                    assertThat(ingressos).hasSize(3);
                    assertThat(ingressos).allMatch(i -> i.getFileira() == null && i.getColuna() == null);
                })
                .verifyComplete();
    }

    @Test
    void criarReservaDePistaExcedendoCapacidadeLancaQuantidadeIndisponivelException() {
        UUID eventoId = UUID.randomUUID();
        Evento evento = eventoPista(eventoId, 10);
        ReservaRequest request = new ReservaRequest(null, 5);
        when(eventoRepository.buscarComLockPorId(eventoId)).thenReturn(Mono.just(evento));
        when(ingressoRepository.expirarReservasVencidas(eventoId)).thenReturn(Mono.just(0));
        when(ingressoRepository.contarAtivosPorEvento(eventoId)).thenReturn(Mono.just(8L));

        StepVerifier.create(reservaService.criar(eventoId, request, UUID.randomUUID()))
                .expectError(QuantidadeIndisponivelException.class)
                .verify();
        verify(ingressoRepository, never()).saveAll(any(List.class));
    }

    @Test
    void criarReservaDeEventoInexistenteLancaEventoNotFoundException() {
        UUID eventoId = UUID.randomUUID();
        when(eventoRepository.buscarComLockPorId(eventoId)).thenReturn(Mono.empty());

        StepVerifier.create(reservaService.criar(eventoId, new ReservaRequest(null, 1), UUID.randomUUID()))
                .expectError(EventoNotFoundException.class)
                .verify();
    }

    private Evento eventoAssentos(UUID id) {
        OffsetDateTime agora = OffsetDateTime.now();
        return new Evento(id, "Peça", CategoriaEvento.TEATRO, "Descrição", "Teatro", agora.plusDays(10),
                FormaVenda.ASSENTOS, 10, 10, 100, new BigDecimal("100.00"), UUID.randomUUID(), agora, agora);
    }

    private Evento eventoPista(UUID id, int quantidadeTotal) {
        OffsetDateTime agora = OffsetDateTime.now();
        return new Evento(id, "Show", CategoriaEvento.SHOW, "Descrição", "Arena", agora.plusDays(10), FormaVenda.PISTA,
                null, null, quantidadeTotal, new BigDecimal("80.00"), UUID.randomUUID(), agora, agora);
    }
}
