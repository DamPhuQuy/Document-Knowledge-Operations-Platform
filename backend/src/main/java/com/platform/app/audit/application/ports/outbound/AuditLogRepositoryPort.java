package com.platform.app.audit.application.ports.outbound;

import com.platform.app.audit.application.dto.AuditLogQueryFilter;
import com.platform.app.audit.domain.model.AuditLog;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditLogRepositoryPort {
    AuditLog save(AuditLog auditLog);

    Optional<AuditLog> findById(UUID id);

    Page<AuditLog> findAll(AuditLogQueryFilter filter, Pageable pageable);
}
