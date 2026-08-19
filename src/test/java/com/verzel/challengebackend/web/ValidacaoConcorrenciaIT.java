package com.verzel.challengebackend.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.verzel.challengebackend.domain.CategoriaEvento;
import com.verzel.challengebackend.domain.Evento;
import com.verzel.challengebackend.domain.FormaVenda;
import com.verzel.challengebackend.domain.Ingresso;
import com.verzel.challengebackend.domain.StatusIngresso;
import com.verzel.challengebackend.repository.EventoRepository;
import com.verzel.challengebackend.repository.IngressoRepository;
import com.verzel.challengebackend.service.ResultadoValidacao;
import com.verzel.challengebackend.service.ValidacaoResultado;
import com.verzel.challengebackend.service.ValidacaoService;
import com.verzel.challengebackend.support.FakePagamentoGatewayConfig;
import com.verzel.challengebackend.support.TestcontainersConfig;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import reactor.core.publisher.Flux;

@SpringBootTest
@Import({TestcontainersConfig.class, FakePagamentoGatewayConfig.class})
class ValidacaoConcorrenciaIT {

    private static final UUID ORGANIZADOR_SEED_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CLIENTE_SEED_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PORTARIA_SEED_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Autowired
    private EventoRepository eventoRepository;
    @Autowired
    private IngressoRepository ingressoRepository;
    @Autowired
    private ValidacaoService validacaoService;

    @Test
    void dezValidacoesConcorrentesNoMesmoIngressoApenasUmaRetornaValido() {
        UUID eventoId = criarEventoERetornarId();
        UUID ingressoId = criarIngressoVendidoERetornarId(eventoId);

        List<ValidacaoResultado> resultados = Flux.range(0, 10)
                .flatMap(i -> validacaoService.validar(eventoId, ingressoId, PORTARIA_SEED_ID), 10)
                .collectList()
                .block();

        long validos = resultados.stream().filter(r -> r.resultado() == ResultadoValidacao.VALIDO).count();
        long jaUtilizados = resultados.stream().filter(r -> r.resultado() == ResultadoValidacao.JA_UTILIZADO).count();
        assertThat(validos).isEqualTo(1);
        assertThat(jaUtilizados).isEqualTo(9);
    }

    private UUID criarEventoERetornarId() {
        UUID id = UUID.randomUUID();
        OffsetDateTime agora = OffsetDateTime.now();
        Evento evento = new Evento(id, "Evento de Validação", CategoriaEvento.SHOW, "Descrição", "Local",
                agora.plusDays(5), FormaVenda.ASSENTOS, 10, 10, 100, new BigDecimal("50.00"), ORGANIZADOR_SEED_ID,
                agora, agora)
                .marcarComoNovo();
        return eventoRepository.save(evento).block().getId();
    }

    private UUID criarIngressoVendidoERetornarId(UUID eventoId) {
        OffsetDateTime agora = OffsetDateTime.now();
        Ingresso ingresso = new Ingresso(UUID.randomUUID(), eventoId, UUID.randomUUID(), CLIENTE_SEED_ID, 1, 1,
                new BigDecimal("50.00"), StatusIngresso.VENDIDO, agora.minusMinutes(20), agora.plusHours(200),
                "pi_teste", agora, agora)
                .marcarComoNovo();
        return ingressoRepository.save(ingresso).block().getId();
    }
}
