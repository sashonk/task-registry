package ru.asocial.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;

import ru.asocial.task.dto.TaskCreateCommand;

@TestConfiguration
public class TestKafkaConfig {

	@Bean
	@SuppressWarnings("unchecked")
	public KafkaTemplate<String, TaskCreateCommand> taskCreateKafkaTemplate() {
		KafkaTemplate<String, TaskCreateCommand> template = mock(KafkaTemplate.class);
		when(template.send(anyString(), anyString(), any(TaskCreateCommand.class)))
				.thenReturn(CompletableFuture.completedFuture(null));
		return template;
	}
}
