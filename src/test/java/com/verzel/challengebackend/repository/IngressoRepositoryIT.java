package com.verzel.challengebackend.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.verzel.challengebackend.domain.CategoriaEvento;
import com.verzel.challengebackend.domain.Evento;
import com.verzel.challengebackend.domain.FormaVenda;
import com.verzel.challengebackend.domain.Ingresso;
import com.verzel.challengebackend.domain.StatusIngresso;
import com.verzel.challengebackend.support.TestcontainersConfig;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import reactor.test.StepVerifier;

@SpringBootTest
@Import(TestcontainersConfig.class)
class IngressoRepositoryIT {

    private static final UUID ORGANIZADOR_SEED_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CLIENTE_SEED_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PORTARIA_SEED_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private IngressoRepository ingressoRepository;

    private UUID eventoAssentosId;

    @BeforeEach
    void criarEventoDeAssentos() {
        OffsetDateTime agora = OffsetDateTime.now();
        Evento evento = new Evento(UUID.randomUUID(), "Show de Teste", CategoriaEvento.SHOW, "Descrição", "Arena",
                agora.plusDays(5), FormaVenda.ASSENTOS, 10, 10, 100, new BigDecimal("100.00"), ORGANIZADOR_SEED_ID,
                agora, agora)
                .marcarComoNovo();
        eventoAssentosId = eventoRepository.save(evento).block().getId();
    }

    @Test
    void salvaEBuscaIngressoDeAssentoReservado() {
        UUID id = UUID.randomUUID();
        UUID reservaId = UUID.randomUUID();
        OffsetDateTime agora = OffsetDateTime.now();
        Ingresso ingresso = new Ingresso(id, eventoAssentosId, reservaId, CLIENTE_SEED_ID, 1, 1,
                new BigDecimal("100.00"), StatusIngresso.RESERVADO, agora.plusMinutes(10), agora.plusHours(200),
                null, agora, agora)
                .marcarComoNovo();

        StepVerifier.create(ingressoRepository.save(ingresso).then(ingressoRepository.findById(id)))
                .assertNext(found -> {
                    assertThat(found.getEventoId()).isEqualTo(eventoAssentosId);
                    assertThat(found.getReservaId()).isEqualTo(reservaId);
                    assertThat(found.getFileira()).isEqualTo(1);
                    assertThat(found.getColuna()).isEqualTo(1);
                    assertThat(found.getStatus()).isEqualTo(StatusIngresso.RESERVADO);
                })
                .verifyComplete();
    }

    @Test
    void reservarOMesmoAssentoDuasVezesVioleOIndiceUnico() {
        OffsetDateTime agora = OffsetDateTime.now();
        Ingresso primeiro = novoIngresso(agora, StatusIngresso.RESERVADO, 3, 4, agora.plusMinutes(10));
        Ingresso segundo = novoIngresso(agora, StatusIngresso.RESERVADO, 3, 4, agora.plusMinutes(10));

        StepVerifier.create(ingressoRepository.save(primeiro).then(ingressoRepository.save(segundo)))
                .expectError(DataIntegrityViolationException.class)
                .verify();
    }

    @Test
    void cancelarUmIngressoLiberaOAssentoParaUmaNovaReserva() {
        OffsetDateTime agora = OffsetDateTime.now();
        Ingresso original = novoIngresso(agora, StatusIngresso.RESERVADO, 5, 5, agora.plusMinutes(10));

        StepVerifier.create(ingressoRepository.save(original)
                        .flatMap(salvo -> ingressoRepository.save(salvo.cancelado(OffsetDateTime.now())))
                        .then(ingressoRepository.save(novoIngresso(agora, StatusIngresso.RESERVADO, 5, 5,
                                agora.plusMinutes(10)))))
                .assertNext(novoAtivo -> assertThat(novoAtivo.getStatus()).isEqualTo(StatusIngresso.RESERVADO))
                .verifyComplete();
    }

    @Test
    void expirarReservasVencidasSoAtualizaReservadosComExpiracaoNoPassado() {
        OffsetDateTime agora = OffsetDateTime.now();
        Ingresso vencido = novoIngresso(agora, StatusIngresso.RESERVADO, 1, 1, agora.minusMinutes(1));
        Ingresso aindaValido = novoIngresso(agora, StatusIngresso.RESERVADO, 1, 2, agora.plusMinutes(10));
        Ingresso jaVendido = novoIngresso(agora, StatusIngresso.VENDIDO, 1, 3, agora.minusMinutes(1));

        StepVerifier.create(ingressoRepository.save(vencido)
                        .then(ingressoRepository.save(aindaValido))
                        .then(ingressoRepository.save(jaVendido))
                        .then(ingressoRepository.expirarReservasVencidas(eventoAssentosId))
                        .then(ingressoRepository.findById(vencido.getId())))
                .assertNext(v -> assertThat(v.getStatus()).isEqualTo(StatusIngresso.EXPIRADA))
                .verifyComplete();

        StepVerifier.create(ingressoRepository.findById(aindaValido.getId()))
                .assertNext(v -> assertThat(v.getStatus()).isEqualTo(StatusIngresso.RESERVADO))
                .verifyComplete();
        StepVerifier.create(ingressoRepository.findById(jaVendido.getId()))
                .assertNext(v -> assertThat(v.getStatus()).isEqualTo(StatusIngresso.VENDIDO))
                .verifyComplete();
    }

    @Test
    void contarAtivosPorEventoIgnoraExpiradosCanceladosEReservasVencidasAindaNaoLimpas() {
        OffsetDateTime agora = OffsetDateTime.now();
        Ingresso vendido = novoIngresso(agora, StatusIngresso.VENDIDO, 1, 1, agora.plusMinutes(10));
        Ingresso reservadoAtivo = novoIngresso(agora, StatusIngresso.RESERVADO, 1, 2, agora.plusMinutes(10));
        Ingresso reservadoVencidoNaoLimpo = novoIngresso(agora, StatusIngresso.RESERVADO, 1, 3, agora.minusMinutes(1));
        Ingresso cancelado = novoIngresso(agora, StatusIngresso.CANCELADA, 1, 4, agora.plusMinutes(10));

        StepVerifier.create(ingressoRepository.save(vendido)
                        .then(ingressoRepository.save(reservadoAtivo))
                        .then(ingressoRepository.save(reservadoVencidoNaoLimpo))
                        .then(ingressoRepository.save(cancelado))
                        .then(ingressoRepository.contarAtivosPorEvento(eventoAssentosId)))
                .assertNext(ativos -> assertThat(ativos).isEqualTo(2L))
                .verifyComplete();
    }

    @Test
    void buscarAssentoAtivoEncontraApenasQuandoOcupado() {
        StepVerifier.create(ingressoRepository.buscarAssentoAtivo(eventoAssentosId, 7, 7))
                .verifyComplete();

        OffsetDateTime agora = OffsetDateTime.now();
        Ingresso ocupando = novoIngresso(agora, StatusIngresso.RESERVADO, 7, 7, agora.plusMinutes(10));
        StepVerifier.create(ingressoRepository.save(ocupando)
                        .then(ingressoRepository.buscarAssentoAtivo(eventoAssentosId, 7, 7)))
                .assertNext(encontrado -> assertThat(encontrado.getId()).isEqualTo(ocupando.getId()))
                .verifyComplete();
    }

    @Test
    void buscarComLockPorIdRetornaOEvento() {
        StepVerifier.create(eventoRepository.buscarComLockPorId(eventoAssentosId))
                .assertNext(evento -> assertThat(evento.getId()).isEqualTo(eventoAssentosId))
                .verifyComplete();
    }

    @Test
    void validarUsoMarcaComoUsadoQuandoVendidoERetornaUmaLinhaAfetada() {
        OffsetDateTime agora = OffsetDateTime.now();
        Ingresso vendido = novoIngresso(agora, StatusIngresso.VENDIDO, 8, 8, agora.plusMinutes(10));
        UUID portariaId = PORTARIA_SEED_ID;

        StepVerifier.create(ingressoRepository.save(vendido)
                        .flatMap(salvo -> ingressoRepository.validarUso(salvo.getId(), portariaId, agora)))
                .assertNext(linhasAfetadas -> assertThat(linhasAfetadas).isEqualTo(1))
                .verifyComplete();

        StepVerifier.create(ingressoRepository.findById(vendido.getId()))
                .assertNext(usado -> {
                    assertThat(usado.getStatus()).isEqualTo(StatusIngresso.USADO);
                    assertThat(usado.getValidadoEm()).isCloseTo(agora, within(1, ChronoUnit.SECONDS));
                    assertThat(usado.getValidadoPorId()).isEqualTo(portariaId);
                })
                .verifyComplete();
    }

    @Test
    void validarUsoNaoAfetaLinhaQuandoIngressoJaNaoEstaVendido() {
        OffsetDateTime agora = OffsetDateTime.now();
        Ingresso vendido = novoIngresso(agora, StatusIngresso.VENDIDO, 9, 9, agora.plusMinutes(10));

        StepVerifier.create(ingressoRepository.save(vendido)
                        .flatMap(salvo -> ingressoRepository.save(salvo.usado(PORTARIA_SEED_ID, agora)))
                        .flatMap(usado -> ingressoRepository.validarUso(usado.getId(), PORTARIA_SEED_ID,
                                OffsetDateTime.now())))
                .assertNext(linhasAfetadas -> assertThat(linhasAfetadas).isEqualTo(0))
                .verifyComplete();
    }

    @Test
    void salvaEBuscaIngressoPorTokenDeCompartilhamento() {
        OffsetDateTime agora = OffsetDateTime.now();
        UUID token = UUID.randomUUID();
        Ingresso vendido = novoIngresso(agora, StatusIngresso.VENDIDO, 6, 6, agora.plusMinutes(10));

        StepVerifier.create(ingressoRepository.save(vendido)
                        .flatMap(salvo -> ingressoRepository.save(salvo.comCompartilhamentoToken(token, agora)))
                        .then(ingressoRepository.findByCompartilhamentoToken(token)))
                .assertNext(encontrado -> assertThat(encontrado.getId()).isEqualTo(vendido.getId()))
                .verifyComplete();
    }

    @Test
    void buscarPorTokenDeCompartilhamentoInexistenteRetornaVazio() {
        StepVerifier.create(ingressoRepository.findByCompartilhamentoToken(UUID.randomUUID()))
                .verifyComplete();
    }

    @Test
    void contarAtivosPorEventoConsideraIngressosUsadosComoAtivos() {
        OffsetDateTime agora = OffsetDateTime.now();
        Ingresso usado = novoIngresso(agora, StatusIngresso.USADO, 2, 1, agora.plusMinutes(10));

        StepVerifier.create(ingressoRepository.save(usado)
                        .then(ingressoRepository.contarAtivosPorEvento(eventoAssentosId)))
                .assertNext(ativos -> assertThat(ativos).isEqualTo(1L))
                .verifyComplete();
    }

    @Test
    void buscarAssentoAtivoConsideraAssentoUsadoComoOcupado() {
        OffsetDateTime agora = OffsetDateTime.now();
        Ingresso usado = novoIngresso(agora, StatusIngresso.USADO, 2, 2, agora.plusMinutes(10));

        StepVerifier.create(ingressoRepository.save(usado)
                        .then(ingressoRepository.buscarAssentoAtivo(eventoAssentosId, 2, 2)))
                .assertNext(encontrado -> assertThat(encontrado.getId()).isEqualTo(usado.getId()))
                .verifyComplete();
    }

    @Test
    void buscarAssentosOcupadosIncluiIngressosUsados() {
        OffsetDateTime agora = OffsetDateTime.now();
        Ingresso usado = novoIngresso(agora, StatusIngresso.USADO, 2, 3, agora.plusMinutes(10));

        StepVerifier.create(ingressoRepository.save(usado)
                        .thenMany(ingressoRepository.buscarAssentosOcupados(eventoAssentosId))
                        .filter(i -> i.getId().equals(usado.getId()))
                        .next())
                .assertNext(encontrado -> assertThat(encontrado.getStatus()).isEqualTo(StatusIngresso.USADO))
                .verifyComplete();
    }

    @Test
    void reservarAssentoJaUsadoVioleOIndiceUnico() {
        OffsetDateTime agora = OffsetDateTime.now();
        Ingresso vendido = novoIngresso(agora, StatusIngresso.VENDIDO, 2, 4, agora.plusMinutes(10));
        Ingresso novaTentativa = novoIngresso(agora, StatusIngresso.RESERVADO, 2, 4, agora.plusMinutes(10));

        StepVerifier.create(ingressoRepository.save(vendido)
                        .flatMap(salvo -> ingressoRepository.save(salvo.usado(PORTARIA_SEED_ID, agora)))
                        .then(ingressoRepository.save(novaTentativa)))
                .expectError(DataIntegrityViolationException.class)
                .verify();
    }

    private Ingresso novoIngresso(OffsetDateTime agora, StatusIngresso status, int fileira, int coluna,
            OffsetDateTime expiraEm) {
        return new Ingresso(UUID.randomUUID(), eventoAssentosId, UUID.randomUUID(), CLIENTE_SEED_ID, fileira, coluna,
                new BigDecimal("100.00"), status, expiraEm, agora.plusHours(200), null, agora, agora)
                .marcarComoNovo();
    }
}
