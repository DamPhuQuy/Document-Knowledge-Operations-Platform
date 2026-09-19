package com.platform.app.audit.infrastructure.adapters.secondary.persistence.entity;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.platform.app.audit.domain.model.AuditStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogJpaEntity {

  @Id
  @Column(name = "id", nullable = false)
  private UUID id;

  @Column(name = "user_id")
  private UUID userId;

  @Column(name = "action", nullable = false, length = 100)
  private String action;

  @Column(name = "resource_type", nullable = false, length = 100)
  private String resourceType;

  @Column(name = "resource_id", length = 64)
  private String resourceId;

  @Column(name = "ip_address", length = 45)
  private String ipAddress;

  @Column(name = "user_agent", columnDefinition = "text")
  private String userAgent;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 50)
  private AuditStatus status;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "details", nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> details;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
}
