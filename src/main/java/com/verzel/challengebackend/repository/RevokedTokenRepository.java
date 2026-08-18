package com.verzel.challengebackend.repository;

import com.verzel.challengebackend.domain.RevokedToken;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface RevokedTokenRepository extends ReactiveCrudRepository<RevokedToken, UUID> {
}
