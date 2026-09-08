package ru.asocial.task.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

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

    public void sendTaskCreatedEvent(Long taskId, String taskType, String parameters) {
        KafkaTemplate<String, String> template = kafkaTemplate.getIfAvailable();
        if (template == null) {
            log.debug("Kafka template not available, skipping audit event for task creation: {}", taskId);
            return;
        }
        try {
            String username = getUsername();
            AuditEventPayload payload = AuditEventPayload.forTaskCreated(
                    taskId, taskType, parameters, username, getIpAddress(), getUserRole());
            template.send(TOPIC, objectMapper.writeValueAsString(payload));
        } catch (JacksonException e) {
            log.warn("Failed to serialize audit event for task creation: {}", taskId, e);
        }
    }

    public void sendTaskStatusChangedEvent(Long taskId, String fromStatus, String toStatus) {
        KafkaTemplate<String, String> template = kafkaTemplate.getIfAvailable();
        if (template == null) {
            log.debug("Kafka template not available, skipping audit event for task status change: {}", taskId);
            return;
        }
        try {
            String username = getUsername();
            AuditEventPayload payload = AuditEventPayload.forTaskStatusChanged(
                    taskId, fromStatus, toStatus, username, getIpAddress(), getUserRole());
            template.send(TOPIC, objectMapper.writeValueAsString(payload));
        } catch (JacksonException e) {
            log.warn("Failed to serialize audit event for task status change: {}", taskId, e);
        }
    }

    private String getUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }

    private String getUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return null;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .findFirst()
                .orElse(null);
    }

    private String getIpAddress() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isEmpty()) {
                return forwarded.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        }
        return null;
    }
}
