package com.verzel.challengebackend.repository;

import com.verzel.challengebackend.domain.Ingresso;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IngressoRepository extends ReactiveCrudRepository<Ingresso, UUID> {

    Flux<Ingresso> findByReservaId(UUID reservaId);

    Mono<Ingresso> findByCompartilhamentoToken(UUID compartilhamentoToken);

    @Query("SELECT * FROM ingressos WHERE comprador_id = :compradorId "
            + "AND (status = 'VENDIDO' OR status = 'USADO') ORDER BY created_at DESC")
    Flux<Ingresso> buscarVendidosPorComprador(@Param("compradorId") UUID compradorId);

    @Modifying
    @Query("UPDATE ingressos SET status = 'EXPIRADA', updated_at = now() "
            + "WHERE evento_id = :eventoId AND status = 'RESERVADO' AND expira_em <= now()")
    Mono<Integer> expirarReservasVencidas(@Param("eventoId") UUID eventoId);

    @Query("SELECT COUNT(*) FROM ingressos WHERE evento_id = :eventoId "
            + "AND (status = 'VENDIDO' OR status = 'USADO' OR (status = 'RESERVADO' AND expira_em > now()))")
    Mono<Long> contarAtivosPorEvento(@Param("eventoId") UUID eventoId);

    @Query("SELECT * FROM ingressos WHERE evento_id = :eventoId AND fileira IS NOT NULL "
            + "AND (status = 'VENDIDO' OR status = 'USADO' OR (status = 'RESERVADO' AND expira_em > now()))")
    Flux<Ingresso> buscarAssentosOcupados(@Param("eventoId") UUID eventoId);

    @Query("SELECT * FROM ingressos WHERE evento_id = :eventoId AND fileira = :fileira AND coluna = :coluna "
            + "AND (status = 'VENDIDO' OR status = 'USADO' OR (status = 'RESERVADO' AND expira_em > now()))")
    Mono<Ingresso> buscarAssentoAtivo(@Param("eventoId") UUID eventoId, @Param("fileira") Integer fileira,
            @Param("coluna") Integer coluna);

    /** Update atômico e condicional — a garantia central de não-validar-duas-vezes. Se duas
     * portarias escanearem o mesmo ingresso ao mesmo tempo, o Postgres serializa as duas
     * UPDATEs na mesma linha; só a primeira a comitar encontra status = 'VENDIDO' e afeta 1
     * linha — a segunda já vê 'USADO' e afeta 0 (ver ValidacaoService). */
    @Modifying
    @Query("UPDATE ingressos SET status = 'USADO', validado_em = :agora, validado_por_id = :portariaId, "
            + "updated_at = :agora WHERE id = :id AND status = 'VENDIDO'")
    Mono<Integer> validarUso(@Param("id") UUID id, @Param("portariaId") UUID portariaId,
            @Param("agora") OffsetDateTime agora);
}
