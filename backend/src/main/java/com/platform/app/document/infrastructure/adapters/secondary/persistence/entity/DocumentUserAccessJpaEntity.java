package com.platform.app.document.infrastructure.adapters.secondary.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import com.platform.app.document.domain.model.PermissionLevel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "document_user_access")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DocumentUserAccessJpaEntity {

  @Id
  @EqualsAndHashCode.Include
  private UUID id;

  @Column(name = "document_id", nullable = false)
  private UUID documentId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "permission_level", nullable = false, length = 50)
  private PermissionLevel permissionLevel;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
}
