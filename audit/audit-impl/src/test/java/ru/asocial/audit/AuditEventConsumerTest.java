package ru.asocial.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import ru.asocial.audit.kafka.AuditEventConsumer;
import ru.asocial.audit.model.AuditEventType;
import ru.asocial.audit.service.AuditService;

@ExtendWith(MockitoExtension.class)
class AuditEventConsumerTest {

    @Mock
    private AuditService auditService;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private AuditEventConsumer consumer;

    @Test
    void shouldConsumeAndRecordLoginEvent() throws JacksonException {
        ObjectMapper mapper = new ObjectMapper();

        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", "LOGIN_SUCCESSFUL");
        payload.put("username", "admin");
        payload.put("ipAddress", "127.0.0.1");
        payload.put("metadata", null);
        payload.put("userRole", "ADMIN");

        String message = mapper.writeValueAsString(payload);

        consumer.consume(message, acknowledgment);

        ArgumentCaptor<AuditEventType> typeCaptor = ArgumentCaptor.forClass(AuditEventType.class);
        verify(auditService).recordEvent(
                typeCaptor.capture(),
                eq("admin"),
                eq("127.0.0.1"),
                eq(null),
                eq("ADMIN"));

        assertThat(typeCaptor.getValue()).isEqualTo(AuditEventType.LOGIN_SUCCESSFUL);
    }

    @Test
    void shouldConsumeAndRecordTaskCreatedEvent() throws JacksonException {
        ObjectMapper mapper = new ObjectMapper();

        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", "TASK_CREATED");
        payload.put("username", "user");
        payload.put("ipAddress", null);
        payload.put("metadata", "{\"taskId\":42,\"taskType\":\"RSS\"}");
        payload.put("userRole", null);

        String message = mapper.writeValueAsString(payload);

        consumer.consume(message, acknowledgment);

        verify(auditService).recordEvent(
                eq(AuditEventType.TASK_CREATED),
                eq("user"),
                eq(null),
                eq("{\"taskId\":42,\"taskType\":\"RSS\"}"),
                eq(null));
    }

    @Test
    void shouldLogErrorForUnknownEventType() throws JacksonException {
        ObjectMapper mapper = new ObjectMapper();

        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", "UNKNOWN_EVENT");
        payload.put("username", "admin");
        payload.put("ipAddress", null);
        payload.put("metadata", null);
        payload.put("userRole", null);

        String message = mapper.writeValueAsString(payload);

        consumer.consume(message, acknowledgment);

        verify(auditService, org.mockito.Mockito.never())
                .recordEvent(any(), any(), any(), any(), any());
    }
}
