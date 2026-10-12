package com.sonofnos.nibssgateway.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Append-only audit trail. Every state-changing operation (transfer
 * initiated, screened, reversed, settled) writes one row here, separate
 * from and in addition to application logs. AuditLogRepository exposes
 * no update or delete methods, and V6__create_audit_log.sql adds a
 * Postgres rule that rejects UPDATE/DELETE on this table outright, so
 * the immutability is enforced at the database, not just by convention.
 */
@Entity
@Table(name = "audit_log")
public class AuditLogEntry {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private String entityId;

    @Column(name = "details", columnDefinition = "text")
    private String details;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected AuditLogEntry() {
    }

    public AuditLogEntry(String eventType, String entityType, String entityId, String details) {
        this.eventType = eventType;
        this.entityType = entityType;
        this.entityId = entityId;
        this.details = details;
    }

    public UUID getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getDetails() {
        return details;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
