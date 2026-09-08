package ru.asocial.task.config;

import java.util.Map;

import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.util.backoff.FixedBackOff;

import ru.asocial.task.exception.BadRequestException;

@Configuration
@EnableKafka
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class KafkaConsumerConfig {

	@Bean
	public ProducerFactory<String, Object> kafkaProducerFactory(KafkaProperties properties) {
		Map<String, Object> config = properties.buildProducerProperties();
		return new DefaultKafkaProducerFactory<>(config, new StringSerializer(),
				new DelegatingByTypeSerializer(Map.of(byte[].class, new ByteArraySerializer())));
	}

	@Bean
	public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> kafkaProducerFactory) {
		return new KafkaTemplate<>(kafkaProducerFactory);
	}

	@Bean
	public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
		DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
		DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, new FixedBackOff(0L, 0L));
		handler.addNotRetryableExceptions(BadRequestException.class);
		handler.setCommitRecovered(true);
		return handler;
	}

	@Bean
	public KafkaTemplate<String, String> auditKafkaTemplate(KafkaProperties properties) {
		Map<String, Object> config = properties.buildProducerProperties();
		config.put(org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
				StringSerializer.class);
		config.put(org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
				StringSerializer.class);
		return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(config));
	}
}
