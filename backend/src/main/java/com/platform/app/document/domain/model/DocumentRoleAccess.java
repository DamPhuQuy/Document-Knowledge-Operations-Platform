package com.platform.app.document.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.platform.app.shared.util.IdGenerator;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
public class DocumentRoleAccess {

  @ToString.Include
  @EqualsAndHashCode.Include
  private final UUID id;

  @ToString.Include
  private final UUID documentId;

  @ToString.Include
  private final UUID roleId;

  @ToString.Include
  private final PermissionLevel permissionLevel;

  private final Instant createdAt;

  public DocumentRoleAccess(
      UUID id,
      UUID documentId,
      UUID roleId,
      PermissionLevel permissionLevel,
      Instant createdAt) {

    this.id = id != null ? id : IdGenerator.nextId();
    this.documentId = Objects.requireNonNull(documentId, "Document ID must not be null");
    this.roleId = Objects.requireNonNull(roleId, "Role ID must not be null");
    this.permissionLevel = Objects.requireNonNull(permissionLevel, "Permission level must not be null");
    this.createdAt = createdAt != null ? createdAt : Instant.now();
  }
}
