package com.verzel.challengebackend.service.tmdb;

import static org.assertj.core.api.Assertions.assertThat;

import com.verzel.challengebackend.domain.CategoriaEvento;
import com.verzel.challengebackend.domain.FormaVenda;
import com.verzel.challengebackend.repository.EventoRepository;
import com.verzel.challengebackend.support.FakeTmdbClientConfig;
import com.verzel.challengebackend.support.TestcontainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import reactor.test.StepVerifier;

@SpringBootTest
@Import({TestcontainersConfig.class, FakeTmdbClientConfig.class})
class TmdbSyncServiceIT {

    @Autowired
    private TmdbSyncService tmdbSyncService;

    @Autowired
    private EventoRepository eventoRepository;

    @Test
    void sincronizarPersisteUmEventoValidoContraOPostgresReal() {
        StepVerifier.create(tmdbSyncService.sincronizar()).verifyComplete();

        StepVerifier.create(eventoRepository.findAll()
                        .filter(evento -> FakeTmdbClientConfig.FILME_FIXO.id().equals(evento.getTmdbId()))
                        .next())
                .assertNext(evento -> {
                    assertThat(evento.getTitulo()).isEqualTo(FakeTmdbClientConfig.FILME_FIXO.title());
                    assertThat(evento.getDescricao()).isEqualTo(FakeTmdbClientConfig.FILME_FIXO.overview());
                    assertThat(evento.getCategoria()).isEqualTo(CategoriaEvento.FILME);
                    assertThat(evento.getFormaVenda()).isEqualTo(FormaVenda.ASSENTOS);
                    assertThat(evento.getFileiras()).isPositive();
                    assertThat(evento.getColunas()).isPositive();
                    assertThat(evento.getQuantidadeTotalIngressos())
                            .isEqualTo(evento.getFileiras() * evento.getColunas());
                    assertThat(evento.getPreco()).isPositive();
                    assertThat(evento.getPosterUrl()).isEqualTo("https://image.tmdb.org/t/p/w500/poster-teste.jpg");
                })
                .verifyComplete();
    }
}
