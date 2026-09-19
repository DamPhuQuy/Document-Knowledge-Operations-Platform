package com.platform.app.audit.infrastructure.adapters.secondary.persistence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.platform.app.audit.infrastructure.adapters.secondary.persistence.entity.AuditLogJpaEntity;

@Repository
public interface SpringDataAuditLogRepository
    extends JpaRepository<AuditLogJpaEntity, UUID>, JpaSpecificationExecutor<AuditLogJpaEntity> {}
