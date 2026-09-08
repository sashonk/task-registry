package ru.asocial.scheduler.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import ru.asocial.task.dto.TaskCreateCommand;

@Configuration
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class KafkaProducerConfig {

	@Bean
	public ProducerFactory<String, TaskCreateCommand> taskCreateProducerFactory(KafkaProperties properties) {
		Map<String, Object> config = new HashMap<>(properties.buildProducerProperties());
		config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
		JacksonJsonSerializer<TaskCreateCommand> serializer = new JacksonJsonSerializer<>();
		return new DefaultKafkaProducerFactory<>(config, new StringSerializer(), serializer);
	}

	@Bean
	public KafkaTemplate<String, TaskCreateCommand> taskCreateKafkaTemplate(
			ProducerFactory<String, TaskCreateCommand> taskCreateProducerFactory) {
		return new KafkaTemplate<>(taskCreateProducerFactory);
	}
}
