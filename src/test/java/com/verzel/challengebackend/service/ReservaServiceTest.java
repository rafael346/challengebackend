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
import com.verzel.challengebackend.service.exception.PagamentoRecusadoException;
import com.verzel.challengebackend.service.exception.QuantidadeIndisponivelException;
import com.verzel.challengebackend.service.exception.ReservaAccessDeniedException;
import com.verzel.challengebackend.service.exception.ReservaExpiradaException;
import com.verzel.challengebackend.service.exception.ReservaNotFoundException;
import com.verzel.challengebackend.service.payment.PagamentoGateway;
import com.verzel.challengebackend.service.payment.PagamentoResultado;
import com.verzel.challengebackend.web.dto.AssentoRequest;
import com.verzel.challengebackend.web.dto.ConfirmarReservaRequest;
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
    private PagamentoGateway pagamentoGateway;
    private ReservaService reservaService;

    @BeforeEach
    void setUp() {
        eventoRepository = mock(EventoRepository.class);
        ingressoRepository = mock(IngressoRepository.class);
        pagamentoGateway = mock(PagamentoGateway.class);
        reservaService = new ReservaService(eventoRepository, ingressoRepository, pagamentoGateway, 600);
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

    @Test
    void confirmarComPagamentoAprovadoMarcaIngressosComoVendidos() {
        UUID reservaId = UUID.randomUUID();
        UUID compradorId = UUID.randomUUID();
        List<Ingresso> itens = List.of(ingressoReservado(reservaId, compradorId, OffsetDateTime.now().plusMinutes(5)));
        when(ingressoRepository.findByReservaId(reservaId)).thenReturn(Flux.fromIterable(itens));
        when(pagamentoGateway.confirmarPagamento(eq("pm_ok"), any(BigDecimal.class), eq(reservaId.toString())))
                .thenReturn(Mono.just(PagamentoResultado.sucesso("pi_123")));
        when(ingressoRepository.saveAll(any(List.class)))
                .thenAnswer(invocation -> Flux.fromIterable((List<Ingresso>) invocation.getArgument(0)));

        StepVerifier.create(reservaService.confirmar(reservaId, new ConfirmarReservaRequest("pm_ok"), compradorId))
                .assertNext(vendidos -> {
                    assertThat(vendidos).hasSize(1);
                    assertThat(vendidos.get(0).getStatus()).isEqualTo(StatusIngresso.VENDIDO);
                    assertThat(vendidos.get(0).getStripePaymentIntentId()).isEqualTo("pi_123");
                })
                .verifyComplete();
    }

    @Test
    void confirmarComPagamentoRecusadoLancaPagamentoRecusadoExceptionEMantemReserva() {
        UUID reservaId = UUID.randomUUID();
        UUID compradorId = UUID.randomUUID();
        List<Ingresso> itens = List.of(ingressoReservado(reservaId, compradorId, OffsetDateTime.now().plusMinutes(5)));
        when(ingressoRepository.findByReservaId(reservaId)).thenReturn(Flux.fromIterable(itens));
        when(pagamentoGateway.confirmarPagamento(eq("pm_recusado"), any(BigDecimal.class), eq(reservaId.toString())))
                .thenReturn(Mono.just(PagamentoResultado.recusado("Cartão recusado")));

        StepVerifier.create(reservaService.confirmar(reservaId, new ConfirmarReservaRequest("pm_recusado"), compradorId))
                .expectError(PagamentoRecusadoException.class)
                .verify();
        verify(ingressoRepository, never()).saveAll(any(List.class));
    }

    @Test
    void confirmarReservaDeOutroCompradorLancaReservaAccessDeniedException() {
        UUID reservaId = UUID.randomUUID();
        List<Ingresso> itens = List.of(ingressoReservado(reservaId, UUID.randomUUID(), OffsetDateTime.now().plusMinutes(5)));
        when(ingressoRepository.findByReservaId(reservaId)).thenReturn(Flux.fromIterable(itens));

        StepVerifier.create(reservaService.confirmar(reservaId, new ConfirmarReservaRequest("pm_ok"), UUID.randomUUID()))
                .expectError(ReservaAccessDeniedException.class)
                .verify();
    }

    @Test
    void confirmarReservaVencidaLancaReservaExpiradaException() {
        UUID reservaId = UUID.randomUUID();
        UUID compradorId = UUID.randomUUID();
        List<Ingresso> itens = List.of(ingressoReservado(reservaId, compradorId, OffsetDateTime.now().minusMinutes(1)));
        when(ingressoRepository.findByReservaId(reservaId)).thenReturn(Flux.fromIterable(itens));

        StepVerifier.create(reservaService.confirmar(reservaId, new ConfirmarReservaRequest("pm_ok"), compradorId))
                .expectError(ReservaExpiradaException.class)
                .verify();
    }

    @Test
    void confirmarReservaInexistenteLancaReservaNotFoundException() {
        UUID reservaId = UUID.randomUUID();
        when(ingressoRepository.findByReservaId(reservaId)).thenReturn(Flux.empty());

        StepVerifier.create(reservaService.confirmar(reservaId, new ConfirmarReservaRequest("pm_ok"), UUID.randomUUID()))
                .expectError(ReservaNotFoundException.class)
                .verify();
    }

    @Test
    void cancelarReservaAtivaMarcaComoCancelada() {
        UUID reservaId = UUID.randomUUID();
        UUID compradorId = UUID.randomUUID();
        List<Ingresso> itens = List.of(ingressoReservado(reservaId, compradorId, OffsetDateTime.now().plusMinutes(5)));
        when(ingressoRepository.findByReservaId(reservaId)).thenReturn(Flux.fromIterable(itens));
        when(ingressoRepository.saveAll(any(List.class)))
                .thenAnswer(invocation -> Flux.fromIterable((List<Ingresso>) invocation.getArgument(0)));

        StepVerifier.create(reservaService.cancelar(reservaId, compradorId)).verifyComplete();
        verify(ingressoRepository).saveAll(any(List.class));
    }

    @Test
    void cancelarReservaDeOutroCompradorLancaReservaAccessDeniedException() {
        UUID reservaId = UUID.randomUUID();
        List<Ingresso> itens = List.of(ingressoReservado(reservaId, UUID.randomUUID(), OffsetDateTime.now().plusMinutes(5)));
        when(ingressoRepository.findByReservaId(reservaId)).thenReturn(Flux.fromIterable(itens));

        StepVerifier.create(reservaService.cancelar(reservaId, UUID.randomUUID()))
                .expectError(ReservaAccessDeniedException.class)
                .verify();
    }

    @Test
    void cancelarReservaJaVendidaLancaReservaExpiradaException() {
        UUID reservaId = UUID.randomUUID();
        UUID compradorId = UUID.randomUUID();
        Ingresso vendido = ingressoReservado(reservaId, compradorId, OffsetDateTime.now().plusMinutes(5))
                .vendido("pi_123", OffsetDateTime.now());
        when(ingressoRepository.findByReservaId(reservaId)).thenReturn(Flux.just(vendido));

        StepVerifier.create(reservaService.cancelar(reservaId, compradorId))
                .expectError(ReservaExpiradaException.class)
                .verify();
    }

    private Ingresso ingressoReservado(UUID reservaId, UUID compradorId, OffsetDateTime expiraEm) {
        OffsetDateTime agora = OffsetDateTime.now();
        return new Ingresso(UUID.randomUUID(), UUID.randomUUID(), reservaId, compradorId, 1, 1,
                new BigDecimal("100.00"), StatusIngresso.RESERVADO, expiraEm, agora.plusHours(200), null, agora, agora);
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

    @Test
    void disponibilidadeDeEventoAssentosRetornaGradeComOcupados() {
        UUID eventoId = UUID.randomUUID();
        Evento evento = eventoAssentos(eventoId);
        Ingresso ocupado = ingressoReservado(UUID.randomUUID(), UUID.randomUUID(), OffsetDateTime.now().plusMinutes(5));
        when(eventoRepository.findById(eventoId)).thenReturn(Mono.just(evento));
        when(ingressoRepository.buscarAssentosOcupados(eventoId)).thenReturn(Flux.just(ocupado));

        StepVerifier.create(reservaService.disponibilidade(eventoId))
                .assertNext(disponibilidade -> {
                    assertThat(disponibilidade.fileiras()).isEqualTo(10);
                    assertThat(disponibilidade.colunas()).isEqualTo(10);
                    assertThat(disponibilidade.assentosOcupados()).hasSize(1);
                    assertThat(disponibilidade.quantidadeDisponivel()).isNull();
                })
                .verifyComplete();
    }

    @Test
    void disponibilidadeDePistaRetornaQuantidadeDisponivel() {
        UUID eventoId = UUID.randomUUID();
        Evento evento = eventoPista(eventoId, 100);
        when(eventoRepository.findById(eventoId)).thenReturn(Mono.just(evento));
        when(ingressoRepository.contarAtivosPorEvento(eventoId)).thenReturn(Mono.just(30L));

        StepVerifier.create(reservaService.disponibilidade(eventoId))
                .assertNext(disponibilidade -> {
                    assertThat(disponibilidade.fileiras()).isNull();
                    assertThat(disponibilidade.assentosOcupados()).isNull();
                    assertThat(disponibilidade.quantidadeDisponivel()).isEqualTo(70);
                })
                .verifyComplete();
    }

    @Test
    void disponibilidadeDeEventoInexistenteLancaEventoNotFoundException() {
        UUID eventoId = UUID.randomUUID();
        when(eventoRepository.findById(eventoId)).thenReturn(Mono.empty());

        StepVerifier.create(reservaService.disponibilidade(eventoId))
                .expectError(EventoNotFoundException.class)
                .verify();
    }
}
