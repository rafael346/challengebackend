package com.verzel.challengebackend.repository;

import com.verzel.challengebackend.domain.Evento;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface EventoRepository extends ReactiveCrudRepository<Evento, UUID> {

    @Query("SELECT * FROM eventos WHERE id = :id FOR UPDATE")
    Mono<Evento> buscarComLockPorId(@Param("id") UUID id);
}
