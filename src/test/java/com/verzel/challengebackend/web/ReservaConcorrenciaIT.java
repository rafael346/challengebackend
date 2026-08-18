package com.verzel.challengebackend.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.verzel.challengebackend.domain.CategoriaEvento;
import com.verzel.challengebackend.domain.Evento;
import com.verzel.challengebackend.domain.FormaVenda;
import com.verzel.challengebackend.domain.Ingresso;
import com.verzel.challengebackend.repository.EventoRepository;
import com.verzel.challengebackend.repository.IngressoRepository;
import com.verzel.challengebackend.service.ReservaService;
import com.verzel.challengebackend.service.exception.AssentoIndisponivelException;
import com.verzel.challengebackend.service.exception.QuantidadeIndisponivelException;
import com.verzel.challengebackend.support.FakePagamentoGatewayConfig;
import com.verzel.challengebackend.support.TestcontainersConfig;
import com.verzel.challengebackend.web.dto.AssentoRequest;
import com.verzel.challengebackend.web.dto.ReservaRequest;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@SpringBootTest
@Import({TestcontainersConfig.class, FakePagamentoGatewayConfig.class})
class ReservaConcorrenciaIT {

    private static final UUID ORGANIZADOR_SEED_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CLIENTE_SEED_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private EventoRepository eventoRepository;
    @Autowired
    private IngressoRepository ingressoRepository;
    @Autowired
    private ReservaService reservaService;

    @Test
    void dezTentativasConcorrentesNoMesmoAssentoApenasUmaVence() {
        Evento evento = criarESalvarEvento(FormaVenda.ASSENTOS, 10, 10, 100);
        ReservaRequest request = new ReservaRequest(List.of(new AssentoRequest(1, 1)), null);

        List<Object> resultados = Flux.range(0, 10)
                .flatMap(i -> reservaService.criar(evento.getId(), request, CLIENTE_SEED_ID)
                        .cast(Object.class)
                        .onErrorResume(AssentoIndisponivelException.class, Mono::just), 10)
                .collectList()
                .block();

        long sucessos = resultados.stream().filter(r -> r instanceof List).count();
        long falhas = resultados.stream().filter(r -> r instanceof AssentoIndisponivelException).count();
        assertThat(sucessos).isEqualTo(1);
        assertThat(falhas).isEqualTo(9);

        StepVerifier.create(ingressoRepository.buscarAssentosOcupados(evento.getId()))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void reservasConcorrentesDePistaNaoUltrapassamACapacidadeTotal() {
        Evento evento = criarESalvarEvento(FormaVenda.PISTA, null, null, 5);
        ReservaRequest request = new ReservaRequest(null, 2);

        List<Object> resultados = Flux.range(0, 5)
                .flatMap(i -> reservaService.criar(evento.getId(), request, CLIENTE_SEED_ID)
                        .cast(Object.class)
                        .onErrorResume(QuantidadeIndisponivelException.class, Mono::just), 5)
                .collectList()
                .block();

        long sucessos = resultados.stream().filter(r -> r instanceof List).count();
        long ingressosReservados = resultados.stream()
                .filter(r -> r instanceof List)
                .mapToLong(r -> ((List<Ingresso>) r).size())
                .sum();
        // Capacidade 5, chunks de 2: no máximo 2 reservas cabem (4 ingressos) — a terceira
        // sempre falharia, não importa a ordem de execução concorrente.
        assertThat(sucessos).isEqualTo(2);
        assertThat(ingressosReservados).isEqualTo(4);

        StepVerifier.create(ingressoRepository.contarAtivosPorEvento(evento.getId()))
                .assertNext(ativos -> assertThat(ativos).isEqualTo(4L))
                .verifyComplete();
    }

    private Evento criarESalvarEvento(FormaVenda formaVenda, Integer fileiras, Integer colunas, int quantidadeTotal) {
        UUID id = UUID.randomUUID();
        OffsetDateTime agora = OffsetDateTime.now();
        Evento evento = new Evento(id, "Evento de Concorrência", CategoriaEvento.SHOW, "Descrição", "Local",
                agora.plusDays(5), formaVenda, fileiras, colunas, quantidadeTotal, new BigDecimal("50.00"),
                ORGANIZADOR_SEED_ID, agora, agora)
                .marcarComoNovo();
        return eventoRepository.save(evento).block();
    }
}
