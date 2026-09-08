package ru.asocial.audit.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ru.asocial.audit.dto.AuditEventDto;
import ru.asocial.audit.dto.PageResponse;
import ru.asocial.audit.service.AuditService;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/events")
    public PageResponse<AuditEventDto> getEvents(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String username,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return auditService.getEvents(eventType, username, page, size);
    }

    @GetMapping("/events/all")
    public ResponseEntity<List<AuditEventDto>> getAllEvents(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String username) {
        return ResponseEntity.ok(auditService.getEvents(eventType, username));
    }
}
