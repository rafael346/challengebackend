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
import com.verzel.challengebackend.repository.EventoRepository;
import com.verzel.challengebackend.service.exception.EventoAccessDeniedException;
import com.verzel.challengebackend.service.exception.EventoNotFoundException;
import com.verzel.challengebackend.service.exception.InvalidEventoException;
import com.verzel.challengebackend.web.dto.EventoRequest;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class EventoServiceTest {

    private EventoRepository eventoRepository;
    private EventoService eventoService;

    @BeforeEach
    void setUp() {
        eventoRepository = mock(EventoRepository.class);
        eventoService = new EventoService(eventoRepository);
    }

    @Test
    void criarComAssentosCalculaQuantidadeTotalComoFileirasVezesColunas() {
        UUID organizerId = UUID.randomUUID();
        EventoRequest request = new EventoRequest("Peça", CategoriaEvento.TEATRO, "Descrição", "Teatro",
                OffsetDateTime.now().plusDays(5), FormaVenda.ASSENTOS, 10, 20, null, new BigDecimal("100.00"));
        when(eventoRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(eventoService.criar(request, organizerId))
                .assertNext(evento -> {
                    assertThat(evento.getFileiras()).isEqualTo(10);
                    assertThat(evento.getColunas()).isEqualTo(20);
                    assertThat(evento.getQuantidadeTotalIngressos()).isEqualTo(200);
                    assertThat(evento.getOrganizerId()).isEqualTo(organizerId);
                })
                .verifyComplete();
    }

    @Test
    void criarComPistaUsaQuantidadeInformadaDiretamente() {
        UUID organizerId = UUID.randomUUID();
        EventoRequest request = new EventoRequest("Show", CategoriaEvento.SHOW, "Descrição", "Arena",
                OffsetDateTime.now().plusDays(5), FormaVenda.PISTA, null, null, 500, new BigDecimal("80.00"));
        when(eventoRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(eventoService.criar(request, organizerId))
                .assertNext(evento -> {
                    assertThat(evento.getFileiras()).isNull();
                    assertThat(evento.getColunas()).isNull();
                    assertThat(evento.getQuantidadeTotalIngressos()).isEqualTo(500);
                })
                .verifyComplete();
    }

    @Test
    void criarComAssentosSemFileirasOuColunasLancaInvalidEventoException() {
        EventoRequest request = new EventoRequest("Peça", CategoriaEvento.TEATRO, "Descrição", "Teatro",
                OffsetDateTime.now().plusDays(5), FormaVenda.ASSENTOS, null, null, null, new BigDecimal("100.00"));

        StepVerifier.create(eventoService.criar(request, UUID.randomUUID()))
                .expectError(InvalidEventoException.class)
                .verify();
    }

    @Test
    void criarComPistaEnviandoFileirasOuColunasLancaInvalidEventoException() {
        EventoRequest request = new EventoRequest("Show", CategoriaEvento.SHOW, "Descrição", "Arena",
                OffsetDateTime.now().plusDays(5), FormaVenda.PISTA, 10, 20, null, new BigDecimal("80.00"));

        StepVerifier.create(eventoService.criar(request, UUID.randomUUID()))
                .expectError(InvalidEventoException.class)
                .verify();
    }

    @Test
    void criarComPistaSemQuantidadeTotalIngressosLancaInvalidEventoException() {
        EventoRequest request = new EventoRequest("Show", CategoriaEvento.SHOW, "Descrição", "Arena",
                OffsetDateTime.now().plusDays(5), FormaVenda.PISTA, null, null, null, new BigDecimal("80.00"));

        StepVerifier.create(eventoService.criar(request, UUID.randomUUID()))
                .expectError(InvalidEventoException.class)
                .verify();
    }

    @Test
    void editarPeloOrganizadorDonoAtualizaComSucesso() {
        UUID id = UUID.randomUUID();
        UUID organizerId = UUID.randomUUID();
        Evento existente = eventoExistente(id, organizerId);
        EventoRequest request = new EventoRequest("Show Atualizado", CategoriaEvento.SHOW, "Nova descrição",
                "Novo local", OffsetDateTime.now().plusDays(20), FormaVenda.PISTA, null, null, 300,
                new BigDecimal("90.00"));
        when(eventoRepository.findById(id)).thenReturn(Mono.just(existente));
        when(eventoRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(eventoService.editar(id, request, organizerId))
                .assertNext(evento -> {
                    assertThat(evento.getTitulo()).isEqualTo("Show Atualizado");
                    assertThat(evento.getQuantidadeTotalIngressos()).isEqualTo(300);
                })
                .verifyComplete();
    }

    @Test
    void editarPorOutroOrganizadorLancaEventoAccessDeniedException() {
        UUID id = UUID.randomUUID();
        UUID donoOriginal = UUID.randomUUID();
        UUID outroOrganizador = UUID.randomUUID();
        Evento existente = eventoExistente(id, donoOriginal);
        EventoRequest request = new EventoRequest("Show Atualizado", CategoriaEvento.SHOW, "Nova descrição",
                "Novo local", OffsetDateTime.now().plusDays(20), FormaVenda.PISTA, null, null, 300,
                new BigDecimal("90.00"));
        when(eventoRepository.findById(id)).thenReturn(Mono.just(existente));

        StepVerifier.create(eventoService.editar(id, request, outroOrganizador))
                .expectError(EventoAccessDeniedException.class)
                .verify();
        verify(eventoRepository, never()).save(any());
    }

    @Test
    void editarIdInexistenteLancaEventoNotFoundException() {
        UUID id = UUID.randomUUID();
        when(eventoRepository.findById(id)).thenReturn(Mono.empty());
        EventoRequest request = new EventoRequest("Show", CategoriaEvento.SHOW, "Descrição", "Arena",
                OffsetDateTime.now().plusDays(5), FormaVenda.PISTA, null, null, 300, new BigDecimal("90.00"));

        StepVerifier.create(eventoService.editar(id, request, UUID.randomUUID()))
                .expectError(EventoNotFoundException.class)
                .verify();
    }

    @Test
    void removerPeloOrganizadorDonoRemoveComSucesso() {
        UUID id = UUID.randomUUID();
        UUID organizerId = UUID.randomUUID();
        Evento existente = eventoExistente(id, organizerId);
        when(eventoRepository.findById(id)).thenReturn(Mono.just(existente));
        when(eventoRepository.delete(existente)).thenReturn(Mono.empty());

        StepVerifier.create(eventoService.remover(id, organizerId)).verifyComplete();
        verify(eventoRepository).delete(existente);
    }

    @Test
    void removerPorOutroOrganizadorLancaEventoAccessDeniedException() {
        UUID id = UUID.randomUUID();
        UUID donoOriginal = UUID.randomUUID();
        Evento existente = eventoExistente(id, donoOriginal);
        when(eventoRepository.findById(id)).thenReturn(Mono.just(existente));

        StepVerifier.create(eventoService.remover(id, UUID.randomUUID()))
                .expectError(EventoAccessDeniedException.class)
                .verify();
        verify(eventoRepository, never()).delete(any(Evento.class));
    }

    @Test
    void removerIdInexistenteLancaEventoNotFoundException() {
        UUID id = UUID.randomUUID();
        when(eventoRepository.findById(id)).thenReturn(Mono.empty());

        StepVerifier.create(eventoService.remover(id, UUID.randomUUID()))
                .expectError(EventoNotFoundException.class)
                .verify();
    }

    private Evento eventoExistente(UUID id, UUID organizerId) {
        OffsetDateTime agora = OffsetDateTime.now();
        return new Evento(id, "Show Original", CategoriaEvento.SHOW, "Descrição original", "Local original",
                agora.plusDays(10), FormaVenda.PISTA, null, null, 100, new BigDecimal("50.00"), organizerId, agora,
                agora);
    }
}
