package com.platform.app.document.infrastructure.adapters.secondary.persistence.adapter;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.platform.app.document.application.ports.outbound.DocumentAclRepositoryPort;
import com.platform.app.document.domain.model.DocumentDepartmentAccess;
import com.platform.app.document.domain.model.DocumentRoleAccess;
import com.platform.app.document.domain.model.DocumentUserAccess;
import com.platform.app.document.domain.model.PermissionLevel;
import com.platform.app.document.infrastructure.adapters.secondary.persistence.entity.DocumentDepartmentAccessJpaEntity;
import com.platform.app.document.infrastructure.adapters.secondary.persistence.entity.DocumentRoleAccessJpaEntity;
import com.platform.app.document.infrastructure.adapters.secondary.persistence.entity.DocumentUserAccessJpaEntity;
import com.platform.app.document.infrastructure.adapters.secondary.persistence.repository.SpringDataDocumentDepartmentAccessRepository;
import com.platform.app.document.infrastructure.adapters.secondary.persistence.repository.SpringDataDocumentRoleAccessRepository;
import com.platform.app.document.infrastructure.adapters.secondary.persistence.repository.SpringDataDocumentUserAccessRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentAclRepositoryAdapter implements DocumentAclRepositoryPort {

  private final SpringDataDocumentUserAccessRepository userAccessRepository;
  private final SpringDataDocumentDepartmentAccessRepository departmentAccessRepository;
  private final SpringDataDocumentRoleAccessRepository roleAccessRepository;

  @Override
  @Transactional
  public void replacePermissions(
      UUID documentId,
      List<DocumentUserAccess> userGrants,
      List<DocumentDepartmentAccess> departmentGrants,
      List<DocumentRoleAccess> roleGrants) {

    Objects.requireNonNull(documentId, "Document ID must not be null");

    // 1. Clear existing grants for this document
    userAccessRepository.deleteByDocumentId(documentId);
    departmentAccessRepository.deleteByDocumentId(documentId);
    roleAccessRepository.deleteByDocumentId(documentId);

    // 2. Insert new user grants
    if (userGrants != null && !userGrants.isEmpty()) {
      List<DocumentUserAccessJpaEntity> userEntities = userGrants.stream()
          .map(this::toEntity)
          .toList();
      userAccessRepository.saveAll(userEntities);
    }

    // 3. Insert new department grants
    if (departmentGrants != null && !departmentGrants.isEmpty()) {
      List<DocumentDepartmentAccessJpaEntity> deptEntities = departmentGrants.stream()
          .map(this::toEntity)
          .toList();
      departmentAccessRepository.saveAll(deptEntities);
    }

    // 4. Insert new role grants
    if (roleGrants != null && !roleGrants.isEmpty()) {
      List<DocumentRoleAccessJpaEntity> roleEntities = roleGrants.stream()
          .map(this::toEntity)
          .toList();
      roleAccessRepository.saveAll(roleEntities);
    }

    log.debug("Replaced permissions for documentId={}: {} users, {} depts, {} roles",
        documentId,
        userGrants != null ? userGrants.size() : 0,
        departmentGrants != null ? departmentGrants.size() : 0,
        roleGrants != null ? roleGrants.size() : 0);
  }

  @Override
  public List<DocumentUserAccess> findUserGrants(UUID documentId) {
    Objects.requireNonNull(documentId, "Document ID must not be null");
    return userAccessRepository.findByDocumentId(documentId).stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public List<DocumentDepartmentAccess> findDepartmentGrants(UUID documentId) {
    Objects.requireNonNull(documentId, "Document ID must not be null");
    return departmentAccessRepository.findByDocumentId(documentId).stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public List<DocumentRoleAccess> findRoleGrants(UUID documentId) {
    Objects.requireNonNull(documentId, "Document ID must not be null");
    return roleAccessRepository.findByDocumentId(documentId).stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public boolean hasUserPermission(UUID documentId, UUID userId, PermissionLevel requiredLevel) {
    if (documentId == null || userId == null || requiredLevel == null) {
      return false;
    }
    List<DocumentUserAccessJpaEntity> grants = userAccessRepository.findByDocumentId(documentId);
    return grants.stream()
        .filter(g -> g.getUserId().equals(userId))
        .anyMatch(g -> isPermissionSatisfied(g.getPermissionLevel(), requiredLevel));
  }

  private boolean isPermissionSatisfied(PermissionLevel granted, PermissionLevel required) {
    if (granted == null || required == null) {
      return false;
    }
    if (granted == PermissionLevel.ADMIN) {
      return true;
    }
    if (granted == PermissionLevel.EDIT) {
      return required == PermissionLevel.EDIT || required == PermissionLevel.VIEW;
    }
    return granted == required;
  }

  public DocumentUserAccessJpaEntity toEntity(DocumentUserAccess domain) {
    if (domain == null) {
      return null;
    }
    return DocumentUserAccessJpaEntity.builder()
        .id(domain.getId())
        .documentId(domain.getDocumentId())
        .userId(domain.getUserId())
        .permissionLevel(domain.getPermissionLevel())
        .createdAt(domain.getCreatedAt())
        .build();
  }

  public DocumentUserAccess toDomain(DocumentUserAccessJpaEntity entity) {
    if (entity == null) {
      return null;
    }
    return DocumentUserAccess.builder()
        .id(entity.getId())
        .documentId(entity.getDocumentId())
        .userId(entity.getUserId())
        .permissionLevel(entity.getPermissionLevel())
        .createdAt(entity.getCreatedAt())
        .build();
  }

  public DocumentDepartmentAccessJpaEntity toEntity(DocumentDepartmentAccess domain) {
    if (domain == null) {
      return null;
    }
    return DocumentDepartmentAccessJpaEntity.builder()
        .id(domain.getId())
        .documentId(domain.getDocumentId())
        .departmentId(domain.getDepartmentId())
        .permissionLevel(domain.getPermissionLevel())
        .createdAt(domain.getCreatedAt())
        .build();
  }

  public DocumentDepartmentAccess toDomain(DocumentDepartmentAccessJpaEntity entity) {
    if (entity == null) {
      return null;
    }
    return DocumentDepartmentAccess.builder()
        .id(entity.getId())
        .documentId(entity.getDocumentId())
        .departmentId(entity.getDepartmentId())
        .permissionLevel(entity.getPermissionLevel())
        .createdAt(entity.getCreatedAt())
        .build();
  }

  public DocumentRoleAccessJpaEntity toEntity(DocumentRoleAccess domain) {
    if (domain == null) {
      return null;
    }
    return DocumentRoleAccessJpaEntity.builder()
        .id(domain.getId())
        .documentId(domain.getDocumentId())
        .roleId(domain.getRoleId())
        .permissionLevel(domain.getPermissionLevel())
        .createdAt(domain.getCreatedAt())
        .build();
  }

  public DocumentRoleAccess toDomain(DocumentRoleAccessJpaEntity entity) {
    if (entity == null) {
      return null;
    }
    return DocumentRoleAccess.builder()
        .id(entity.getId())
        .documentId(entity.getDocumentId())
        .roleId(entity.getRoleId())
        .permissionLevel(entity.getPermissionLevel())
        .createdAt(entity.getCreatedAt())
        .build();
  }
}
