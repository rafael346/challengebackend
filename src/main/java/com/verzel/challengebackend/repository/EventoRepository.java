package com.verzel.challengebackend.repository;

import com.verzel.challengebackend.domain.Evento;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface EventoRepository extends ReactiveCrudRepository<Evento, UUID> {
}
