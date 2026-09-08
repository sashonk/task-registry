package ru.asocial.audit.kafka;

import java.time.Instant;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import ru.asocial.audit.model.AuditEventType;
import ru.asocial.audit.service.AuditService;

@Component
public class AuditEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditEventConsumer.class);
    private static final String TOPIC = "audit";

    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public AuditEventConsumer(AuditService auditService) {
        this.auditService = auditService;
        this.objectMapper = new ObjectMapper();
    }

    @KafkaListener(topics = TOPIC, groupId = "audit-service")
    public void consume(String message, Acknowledgment ack) {
        try {
            Map<String, Object> data = objectMapper.readValue(message, new TypeReference<>() {
            });

            String eventType = (String) data.get("eventType");
            String username = (String) data.get("username");
            String ipAddress = (String) data.get("ipAddress");
            String metadata = (String) data.get("metadata");
            String userRole = (String) data.get("userRole");

            AuditEventType auditEventType = AuditEventType.valueOf(eventType);

            auditService.recordEvent(auditEventType, username, ipAddress, metadata, userRole);

            log.info("Audit event recorded: {} by {}", eventType, username);
        } catch (IllegalArgumentException e) {
            log.error("Unknown audit event type in message: {}", message, e);
        } catch (Exception e) {
            log.error("Failed to process audit event: {}", message, e);
        } finally {
            ack.acknowledge();
        }
    }
}
