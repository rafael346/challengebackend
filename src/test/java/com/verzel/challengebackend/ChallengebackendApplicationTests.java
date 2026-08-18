package com.verzel.challengebackend;

import com.verzel.challengebackend.support.TestcontainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfig.class)
class ChallengebackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
