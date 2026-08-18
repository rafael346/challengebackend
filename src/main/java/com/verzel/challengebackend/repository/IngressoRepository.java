package com.verzel.challengebackend.repository;

import com.verzel.challengebackend.domain.Ingresso;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IngressoRepository extends ReactiveCrudRepository<Ingresso, UUID> {

    Flux<Ingresso> findByReservaId(UUID reservaId);

    @Modifying
    @Query("UPDATE ingressos SET status = 'EXPIRADA', updated_at = now() "
            + "WHERE evento_id = :eventoId AND status = 'RESERVADO' AND expira_em <= now()")
    Mono<Integer> expirarReservasVencidas(@Param("eventoId") UUID eventoId);

    @Query("SELECT COUNT(*) FROM ingressos WHERE evento_id = :eventoId "
            + "AND (status = 'VENDIDO' OR (status = 'RESERVADO' AND expira_em > now()))")
    Mono<Long> contarAtivosPorEvento(@Param("eventoId") UUID eventoId);

    @Query("SELECT * FROM ingressos WHERE evento_id = :eventoId AND fileira IS NOT NULL "
            + "AND (status = 'VENDIDO' OR (status = 'RESERVADO' AND expira_em > now()))")
    Flux<Ingresso> buscarAssentosOcupados(@Param("eventoId") UUID eventoId);

    @Query("SELECT * FROM ingressos WHERE evento_id = :eventoId AND fileira = :fileira AND coluna = :coluna "
            + "AND (status = 'VENDIDO' OR (status = 'RESERVADO' AND expira_em > now()))")
    Mono<Ingresso> buscarAssentoAtivo(@Param("eventoId") UUID eventoId, @Param("fileira") Integer fileira,
            @Param("coluna") Integer coluna);
}
