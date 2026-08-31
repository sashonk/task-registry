package ru.asocial.task;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestSecurityConfig.class)
class TaskApplicationTests {

	@Test
	void contextLoads() {
	}

}
