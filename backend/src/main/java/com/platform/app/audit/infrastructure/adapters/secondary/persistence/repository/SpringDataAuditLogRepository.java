package com.platform.app.audit.infrastructure.adapters.secondary.persistence.repository;

import com.platform.app.audit.infrastructure.adapters.secondary.persistence.entity.AuditLogJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataAuditLogRepository
    extends
        JpaRepository<AuditLogJpaEntity, UUID>,
        JpaSpecificationExecutor<AuditLogJpaEntity> {}
