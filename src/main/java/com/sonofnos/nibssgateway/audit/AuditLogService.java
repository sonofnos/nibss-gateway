package com.sonofnos.nibssgateway.audit;

import com.sonofnos.nibssgateway.domain.AuditLogEntry;
import com.sonofnos.nibssgateway.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

    private final AuditLogRepository repository;

    public AuditLogService(AuditLogRepository repository) {
        this.repository = repository;
    }

    public void record(String eventType, String entityType, String entityId, String details) {
        repository.save(new AuditLogEntry(eventType, entityType, entityId, details));
    }
}
