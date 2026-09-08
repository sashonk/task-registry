package ru.asocial.audit.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant timestamp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AuditEventType eventType;

    @Column(nullable = false, length = 64)
    private String username;

    @Column(length = 45)
    private String ipAddress;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column(length = 16)
    private String userRole;

    protected AuditEvent() {
    }

    public AuditEvent(Instant timestamp, AuditEventType eventType, String username,
                      String ipAddress, String metadata, String userRole) {
        this.timestamp = timestamp;
        this.eventType = eventType;
        this.username = username;
        this.ipAddress = ipAddress;
        this.metadata = metadata;
        this.userRole = userRole;
    }

    public Long getId() {
        return id;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public AuditEventType getEventType() {
        return eventType;
    }

    public String getUsername() {
        return username;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getMetadata() {
        return metadata;
    }

    public String getUserRole() {
        return userRole;
    }
}
