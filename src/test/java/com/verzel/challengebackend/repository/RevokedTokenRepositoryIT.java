package com.verzel.challengebackend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.verzel.challengebackend.domain.RevokedToken;
import com.verzel.challengebackend.support.TestcontainersConfig;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import reactor.test.StepVerifier;

@SpringBootTest
@Import(TestcontainersConfig.class)
class RevokedTokenRepositoryIT {

    @Autowired
    private RevokedTokenRepository revokedTokenRepository;

    @Test
    void savesAndChecksExistenceOfARevokedToken() {
        UUID jti = UUID.randomUUID();
        RevokedToken revokedToken = new RevokedToken(
                jti, UUID.randomUUID(), OffsetDateTime.now().plusHours(1), OffsetDateTime.now());

        StepVerifier.create(revokedTokenRepository.save(revokedToken).then(revokedTokenRepository.existsById(jti)))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void aTokenThatWasNeverRevokedDoesNotExist() {
        StepVerifier.create(revokedTokenRepository.existsById(UUID.randomUUID()))
                .expectNext(false)
                .verifyComplete();
    }
}
