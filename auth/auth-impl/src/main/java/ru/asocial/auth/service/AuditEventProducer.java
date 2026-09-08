package ru.asocial.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import ru.asocial.audit.dto.AuditEventPayload;

@Component
public class AuditEventProducer {

    private static final Logger log = LoggerFactory.getLogger(AuditEventProducer.class);
    private static final String TOPIC = "audit";

    private final ObjectProvider<KafkaTemplate<String, String>> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuditEventProducer(ObjectProvider<KafkaTemplate<String, String>> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendLoginEvent(String username, String ipAddress, String userRole) {
        KafkaTemplate<String, String> template = kafkaTemplate.getIfAvailable();
        if (template == null) {
            log.debug("Kafka template not available, skipping audit event for login: {}", username);
            return;
        }
        try {
            AuditEventPayload payload = AuditEventPayload.forLogin(username, ipAddress, userRole);
            template.send(TOPIC, objectMapper.writeValueAsString(payload));
        } catch (JacksonException e) {
            log.warn("Failed to serialize audit event for login: {}", username, e);
        }
    }
}
