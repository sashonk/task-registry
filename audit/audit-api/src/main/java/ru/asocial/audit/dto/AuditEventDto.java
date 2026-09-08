package ru.asocial.audit.dto;

import java.time.Instant;

public record AuditEventDto(
        Long id,
        Instant timestamp,
        String eventType,
        String username,
        String ipAddress,
        String metadata,
        String userRole) {
}
