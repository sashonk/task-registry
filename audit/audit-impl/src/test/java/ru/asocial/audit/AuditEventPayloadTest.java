package ru.asocial.audit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import ru.asocial.audit.dto.AuditEventPayload;

class AuditEventPayloadTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void forTaskCreatedIncludesParameters() throws JacksonException {
        AuditEventPayload payload = AuditEventPayload.forTaskCreated(
                42L, "DELAY", "{\"durationSeconds\":5}", "user", "127.0.0.1", "ADMIN");

        assertThat(payload.eventType()).isEqualTo("TASK_CREATED");
        assertThat(payload.username()).isEqualTo("user");
        assertThat(payload.ipAddress()).isEqualTo("127.0.0.1");
        assertThat(payload.userRole()).isEqualTo("ADMIN");

        JsonNode metadata = mapper.readTree(payload.metadata());
        assertThat(metadata.get("taskId").asLong()).isEqualTo(42L);
        assertThat(metadata.get("taskType").asText()).isEqualTo("DELAY");
        assertThat(metadata.get("parameters").asText()).isEqualTo("{\"durationSeconds\":5}");
    }

    @Test
    void forTaskCreatedOmitsParametersWhenNull() throws JacksonException {
        AuditEventPayload payload = AuditEventPayload.forTaskCreated(
                7L, "CLEANUP", null, "admin", "10.0.0.1", "VIEWER");

        JsonNode metadata = mapper.readTree(payload.metadata());
        assertThat(metadata.get("taskId").asLong()).isEqualTo(7L);
        assertThat(metadata.get("taskType").asText()).isEqualTo("CLEANUP");
        assertThat(metadata.has("parameters")).isFalse();
    }

    @Test
    void forTaskCreatedOmitsParametersWhenBlank() throws JacksonException {
        AuditEventPayload payload = AuditEventPayload.forTaskCreated(
                7L, "CLEANUP", "   ", "admin", null, null);

        JsonNode metadata = mapper.readTree(payload.metadata());
        assertThat(metadata.has("parameters")).isFalse();
        assertThat(payload.ipAddress()).isNull();
        assertThat(payload.userRole()).isNull();
    }

    @Test
    void forTaskCreatedEscapesQuotesInParameters() throws JacksonException {
        AuditEventPayload payload = AuditEventPayload.forTaskCreated(
                1L, "TEXT_TRANSFORM", "say \"hi\" \\ done", "user", "127.0.0.1", "ADMIN");

        JsonNode metadata = mapper.readTree(payload.metadata());
        assertThat(metadata.get("parameters").asText()).isEqualTo("say \"hi\" \\ done");
    }

    @Test
    void forTaskCreatedProducesValidJsonForMultilineParameters() throws JacksonException {
        AuditEventPayload payload = AuditEventPayload.forTaskCreated(
                1L, "SCRIPT", "line1\nline2\ttabbed", "user", null, null);

        JsonNode metadata = mapper.readTree(payload.metadata());
        assertThat(metadata.get("parameters").asText()).isEqualTo("line1\nline2\ttabbed");
    }

    @Test
    void forLoginHasEmptyPayload() {
        AuditEventPayload payload = AuditEventPayload.forLogin("admin", "127.0.0.1", "ADMIN");

        assertThat(payload.eventType()).isEqualTo("LOGIN_SUCCESSFUL");
        assertThat(payload.metadata()).isNull();
    }
}
