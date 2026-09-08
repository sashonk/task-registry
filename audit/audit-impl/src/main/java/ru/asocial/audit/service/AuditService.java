package ru.asocial.audit.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.asocial.audit.dto.AuditEventDto;
import ru.asocial.audit.dto.PageResponse;
import ru.asocial.audit.model.AuditEvent;
import ru.asocial.audit.model.AuditEventType;
import ru.asocial.audit.repository.AuditEventRepository;

@Service
public class AuditService {

    private final AuditEventRepository repository;

    public AuditService(AuditEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void recordEvent(AuditEventType eventType, String username, String ipAddress,
                            String metadata, String userRole) {
        AuditEvent event = new AuditEvent(
                java.time.Instant.now(), eventType, username, ipAddress, metadata, userRole);
        repository.save(event);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditEventDto> getEvents(String eventType, String username,
                                                  Integer page, Integer size) {
        int normalizedPage = PageResponse.normalizePage(page != null ? page : 1);
        int normalizedSize = PageResponse.normalizeSize(size != null ? size : 20);
        Pageable pageable = PageRequest.of(normalizedPage - 1, normalizedSize);

        AuditEventType type = resolveEventType(eventType);
        if (type == null && isNotBlank(eventType)) {
            return PageResponse.from(Page.empty(pageable));
        }

        Page<AuditEventDto> result = repository.findFiltered(type, username, null, null, pageable)
                .map(this::toDto);

        return PageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public List<AuditEventDto> getEvents(String eventType, String username) {
        AuditEventType type = resolveEventType(eventType);
        if (type == null && isNotBlank(eventType)) {
            return List.of();
        }
        return repository.findFiltered(type, username).stream()
                .map(this::toDto)
                .toList();
    }

    private AuditEventType resolveEventType(String eventType) {
        if (eventType == null || eventType.isBlank()) {
            return null;
        }
        try {
            return AuditEventType.valueOf(eventType.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
// ...existing code...

    private AuditEventDto toDto(AuditEvent event) {
        return new AuditEventDto(
                event.getId(),
                event.getTimestamp(),
                event.getEventType().name(),
                event.getUsername(),
                event.getIpAddress(),
                event.getMetadata(),
                event.getUserRole());
    }
}
