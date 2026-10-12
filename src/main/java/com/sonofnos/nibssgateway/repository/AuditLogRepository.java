package com.sonofnos.nibssgateway.repository;

import com.sonofnos.nibssgateway.domain.AuditLogEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Deliberately exposes no update/delete beyond what JpaRepository
 * provides by default for other entities - no custom update query is
 * ever added here. See AuditLogEntry for the DB-level enforcement too.
 */
public interface AuditLogRepository extends JpaRepository<AuditLogEntry, UUID> {

    List<AuditLogEntry> findByEntityTypeAndEntityId(String entityType, String entityId);
}
