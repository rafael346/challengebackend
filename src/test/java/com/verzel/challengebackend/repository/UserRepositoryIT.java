package com.verzel.challengebackend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.verzel.challengebackend.domain.TipoAcesso;
import com.verzel.challengebackend.domain.User;
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
class UserRepositoryIT {

    @Autowired
    private UserRepository userRepository;

    @Test
    void savesAndFindsUserByEmail() {
        User user = new User(UUID.randomUUID(), "Teste", "Sobrenome", TipoAcesso.CLIENTE,
                "teste-" + UUID.randomUUID() + "@example.com", "hash", OffsetDateTime.now());

        StepVerifier.create(userRepository.save(user).then(userRepository.findByEmail(user.getEmail())))
                .assertNext(found -> {
                    assertThat(found.getEmail()).isEqualTo(user.getEmail());
                    assertThat(found.getTipoAcesso()).isEqualTo(TipoAcesso.CLIENTE);
                })
                .verifyComplete();
    }

    @Test
    void seedDataHasOneUserPerTipoAcesso() {
        StepVerifier.create(userRepository.findByEmail("organizador@verzel.com"))
                .assertNext(user -> assertThat(user.getTipoAcesso()).isEqualTo(TipoAcesso.ORGANIZADOR))
                .verifyComplete();

        StepVerifier.create(userRepository.findByEmail("cliente@verzel.com"))
                .assertNext(user -> assertThat(user.getTipoAcesso()).isEqualTo(TipoAcesso.CLIENTE))
                .verifyComplete();

        StepVerifier.create(userRepository.findByEmail("portaria@verzel.com"))
                .assertNext(user -> assertThat(user.getTipoAcesso()).isEqualTo(TipoAcesso.PORTARIA))
                .verifyComplete();
    }
}
