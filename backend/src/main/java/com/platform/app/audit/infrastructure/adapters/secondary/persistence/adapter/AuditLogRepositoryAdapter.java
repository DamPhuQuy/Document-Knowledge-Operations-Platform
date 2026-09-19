package com.platform.app.audit.infrastructure.adapters.secondary.persistence.adapter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.platform.app.audit.application.dto.AuditLogQueryFilter;
import com.platform.app.audit.application.ports.outbound.AuditLogRepositoryPort;
import com.platform.app.audit.domain.model.AuditLog;
import com.platform.app.audit.infrastructure.adapters.secondary.persistence.entity.AuditLogJpaEntity;
import com.platform.app.audit.infrastructure.adapters.secondary.persistence.repository.SpringDataAuditLogRepository;
import com.platform.app.audit.infrastructure.adapters.secondary.persistence.specification.AuditLogSpecification;
import com.platform.app.shared.util.IdGenerator;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuditLogRepositoryAdapter implements AuditLogRepositoryPort {

  private final SpringDataAuditLogRepository repository;

  @Override
  public AuditLog save(AuditLog auditLog) {
    if (auditLog == null) {
      return null;
    }

    UUID id = auditLog.getId() != null ? auditLog.getId() : IdGenerator.nextId();
    AuditLogJpaEntity entity = toEntity(auditLog, id);
    AuditLogJpaEntity saved = repository.save(entity);
    return toDomain(saved);
  }

  @Override
  public Optional<AuditLog> findById(UUID id) {
    if (id == null) {
      return Optional.empty();
    }
    return repository.findById(id).map(this::toDomain);
  }

  @Override
  public Page<AuditLog> findAll(AuditLogQueryFilter filter, Pageable pageable) {
    return repository
        .findAll(AuditLogSpecification.withFilter(filter), pageable)
        .map(this::toDomain);
  }

  public AuditLogJpaEntity toEntity(AuditLog domain, UUID id) {
    if (domain == null) {
      return null;
    }
    return AuditLogJpaEntity.builder()
        .id(id)
        .userId(domain.getUserId())
        .action(domain.getAction())
        .resourceType(domain.getResourceType())
        .resourceId(domain.getResourceId())
        .ipAddress(domain.getIpAddress())
        .userAgent(domain.getUserAgent())
        .status(domain.getStatus())
        .details(domain.getDetails())
        .createdAt(domain.getCreatedAt())
        .build();
  }

  public AuditLog toDomain(AuditLogJpaEntity entity) {
    if (entity == null) {
      return null;
    }
    return AuditLog.builder()
        .id(entity.getId())
        .userId(entity.getUserId())
        .action(entity.getAction())
        .resourceType(entity.getResourceType())
        .resourceId(entity.getResourceId())
        .ipAddress(entity.getIpAddress())
        .userAgent(entity.getUserAgent())
        .status(entity.getStatus())
        .details(entity.getDetails())
        .createdAt(entity.getCreatedAt())
        .build();
  }
}
