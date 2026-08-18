package com.verzel.challengebackend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.verzel.challengebackend.domain.CategoriaEvento;
import com.verzel.challengebackend.domain.Evento;
import com.verzel.challengebackend.domain.FormaVenda;
import com.verzel.challengebackend.support.TestcontainersConfig;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import reactor.test.StepVerifier;

@SpringBootTest
@Import(TestcontainersConfig.class)
class EventoRepositoryIT {

    private static final UUID ORGANIZADOR_SEED_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private EventoRepository eventoRepository;

    @Test
    void savesAndFindsEventoWithAssentos() {
        UUID id = UUID.randomUUID();
        OffsetDateTime agora = OffsetDateTime.now();
        Evento evento = new Evento(id, "Peça de Teatro", CategoriaEvento.TEATRO, "Descrição", "Teatro Municipal",
                agora.plusDays(5), FormaVenda.ASSENTOS, 10, 20, 200, new BigDecimal("150.00"),
                ORGANIZADOR_SEED_ID, agora, agora)
                .marcarComoNovo();

        StepVerifier.create(eventoRepository.save(evento).then(eventoRepository.findById(id)))
                .assertNext(found -> {
                    assertThat(found.getTitulo()).isEqualTo("Peça de Teatro");
                    assertThat(found.getCategoria()).isEqualTo(CategoriaEvento.TEATRO);
                    assertThat(found.getFormaVenda()).isEqualTo(FormaVenda.ASSENTOS);
                    assertThat(found.getFileiras()).isEqualTo(10);
                    assertThat(found.getColunas()).isEqualTo(20);
                    assertThat(found.getQuantidadeTotalIngressos()).isEqualTo(200);
                    assertThat(found.getOrganizerId()).isEqualTo(ORGANIZADOR_SEED_ID);
                })
                .verifyComplete();
    }

    @Test
    void savingAnEventoWithIsNewFalseUpdatesInsteadOfInserting() {
        UUID id = UUID.randomUUID();
        OffsetDateTime agora = OffsetDateTime.now();
        Evento original = new Evento(id, "Show Original", CategoriaEvento.SHOW, "Descrição", "Local A",
                agora.plusDays(10), FormaVenda.PISTA, null, null, 100, new BigDecimal("50.00"),
                ORGANIZADOR_SEED_ID, agora, agora)
                .marcarComoNovo();
        Evento atualizado = new Evento(id, "Show Atualizado", CategoriaEvento.SHOW, "Descrição", "Local A",
                agora.plusDays(10), FormaVenda.PISTA, null, null, 100, new BigDecimal("75.00"),
                ORGANIZADOR_SEED_ID, agora, OffsetDateTime.now());

        StepVerifier.create(eventoRepository.count()
                        .flatMap(antes -> eventoRepository.save(original)
                                .then(eventoRepository.save(atualizado))
                                .then(eventoRepository.findById(id))
                                .flatMap(evento -> eventoRepository.count().map(depois -> {
                                    assertThat(depois).isEqualTo(antes + 1);
                                    return evento;
                                }))))
                .assertNext(evento -> {
                    assertThat(evento.getTitulo()).isEqualTo("Show Atualizado");
                    assertThat(evento.getPreco()).isEqualByComparingTo("75.00");
                })
                .verifyComplete();
    }
}
