package ru.asocial.audit.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ru.asocial.audit.model.AuditEvent;
import ru.asocial.audit.model.AuditEventType;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    @Query("SELECT a FROM AuditEvent a WHERE "
            + "(:eventType IS NULL OR a.eventType = :eventType) "
            + "AND (:username IS NULL OR a.username = :username) "
            + "AND (:from IS NULL OR a.timestamp >= :from) "
            + "AND (:to IS NULL OR a.timestamp <= :to) "
            + "ORDER BY a.timestamp DESC, a.id DESC")
    Page<AuditEvent> findFiltered(
            @Param("eventType") AuditEventType eventType,
            @Param("username") String username,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    @Query("SELECT a FROM AuditEvent a WHERE "
            + "(:eventType IS NULL OR a.eventType = :eventType) "
            + "AND (:username IS NULL OR a.username = :username) "
            + "ORDER BY a.timestamp DESC, a.id DESC")
    List<AuditEvent> findFiltered(
            @Param("eventType") AuditEventType eventType,
            @Param("username") String username);
}
