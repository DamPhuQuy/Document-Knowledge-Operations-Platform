package com.platform.app.document.application.services;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.platform.app.document.application.dto.ConfigureDocumentAclCommand;
import com.platform.app.document.application.dto.DepartmentGrantDto;
import com.platform.app.document.application.dto.DocumentPermissionsResponseDto;
import com.platform.app.document.application.dto.RoleGrantDto;
import com.platform.app.document.application.dto.UserGrantDto;
import com.platform.app.document.application.event.DocumentAclUpdatedEvent;
import com.platform.app.document.application.ports.inbound.ConfigureDocumentAclUseCase;
import com.platform.app.document.application.ports.inbound.GetDocumentPermissionsUseCase;
import com.platform.app.document.application.ports.outbound.DocumentAclRepositoryPort;
import com.platform.app.document.application.ports.outbound.DocumentRepositoryPort;
import com.platform.app.document.domain.exception.DocumentAccessDeniedException;
import com.platform.app.document.domain.exception.DocumentNotFoundException;
import com.platform.app.document.domain.exception.DocumentValidationException;
import com.platform.app.document.domain.model.AccessLevel;
import com.platform.app.document.domain.model.Document;
import com.platform.app.document.domain.model.DocumentDepartmentAccess;
import com.platform.app.document.domain.model.DocumentRoleAccess;
import com.platform.app.document.domain.model.DocumentUserAccess;
import com.platform.app.document.domain.model.PermissionLevel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentAclService implements ConfigureDocumentAclUseCase, GetDocumentPermissionsUseCase {

  private final DocumentRepositoryPort documentRepositoryPort;
  private final DocumentAclRepositoryPort documentAclRepositoryPort;
  private final ApplicationEventPublisher eventPublisher;

  @Override
  @Transactional
  public DocumentPermissionsResponseDto configureAcl(ConfigureDocumentAclCommand command) {
    Objects.requireNonNull(command, "Configure ACL command must not be null");
    Objects.requireNonNull(command.getDocumentId(), "Document ID must not be null");
    Objects.requireNonNull(command.getCurrentUserId(), "Current user ID must not be null");

    if (command.getAccessLevel() == null) {
      throw new DocumentValidationException("Access level must not be null");
    }

    Document document = documentRepositoryPort.findById(command.getDocumentId())
        .orElseThrow(() -> new DocumentNotFoundException("Document not found with ID: " + command.getDocumentId()));

    boolean isOwner = document.getUploadedByUserId().equals(command.getCurrentUserId());
    boolean isAuthorized = isOwner || command.isAdmin() || command.isHasManagePermissions();

    if (!isAuthorized) {
      log.warn("Access denied for user {} attempting to configure ACL on document {} owned by {}",
          command.getCurrentUserId(), document.getId(), document.getUploadedByUserId());
      throw new DocumentAccessDeniedException("User does not have permission to configure ACL for document " + command.getDocumentId());
    }

    List<DocumentUserAccess> userGrants = command.getUserGrants() != null
        ? command.getUserGrants().stream()
            .map(g -> DocumentUserAccess.builder()
                .documentId(document.getId())
                .userId(g.getUserId())
                .permissionLevel(g.getPermissionLevel())
                .build())
            .collect(Collectors.toList())
        : Collections.emptyList();

    List<DocumentDepartmentAccess> deptGrants = command.getDepartmentGrants() != null
        ? command.getDepartmentGrants().stream()
            .map(g -> DocumentDepartmentAccess.builder()
                .documentId(document.getId())
                .departmentId(g.getDepartmentId())
                .permissionLevel(g.getPermissionLevel())
                .build())
            .collect(Collectors.toList())
        : Collections.emptyList();

    List<DocumentRoleAccess> roleGrants = command.getRoleGrants() != null
        ? command.getRoleGrants().stream()
            .map(g -> DocumentRoleAccess.builder()
                .documentId(document.getId())
                .roleId(g.getRoleId())
                .permissionLevel(g.getPermissionLevel())
                .build())
            .collect(Collectors.toList())
        : Collections.emptyList();

    // 1. Update Document access classification
    document.updateAccessLevel(command.getAccessLevel());
    Document savedDoc = documentRepositoryPort.save(document);

    // 2. Synchronize explicit ACL entries in single transaction
    documentAclRepositoryPort.replacePermissions(savedDoc.getId(), userGrants, deptGrants, roleGrants);

    // 3. Emit domain event for audit trail logging (UC-AUDIT-01)
    DocumentAclUpdatedEvent event = DocumentAclUpdatedEvent.builder()
        .documentId(savedDoc.getId())
        .updatedByUserId(command.getCurrentUserId())
        .accessLevel(savedDoc.getAccessLevel())
        .userGrantsCount(userGrants.size())
        .departmentGrantsCount(deptGrants.size())
        .roleGrantsCount(roleGrants.size())
        .timestamp(Instant.now())
        .build();
    eventPublisher.publishEvent(event);

    log.info("Successfully updated ACL for document {}: level={}, users={}, depts={}, roles={}",
        savedDoc.getId(), savedDoc.getAccessLevel(), userGrants.size(), deptGrants.size(), roleGrants.size());

    return DocumentPermissionsResponseDto.builder()
        .documentId(savedDoc.getId())
        .accessLevel(savedDoc.getAccessLevel())
        .userGrants(toUserGrantDtos(userGrants))
        .departmentGrants(toDeptGrantDtos(deptGrants))
        .roleGrants(toRoleGrantDtos(roleGrants))
        .updatedAt(savedDoc.getUpdatedAt())
        .build();
  }

  @Override
  @Transactional(readOnly = true)
  public DocumentPermissionsResponseDto getPermissions(
      UUID documentId,
      UUID currentUserId,
      boolean isAdmin,
      boolean hasManagePermissions) {

    Objects.requireNonNull(documentId, "Document ID must not be null");

    Document document = documentRepositoryPort.findById(documentId)
        .orElseThrow(() -> new DocumentNotFoundException("Document not found with ID: " + documentId));

    boolean isOwner = currentUserId != null && document.getUploadedByUserId().equals(currentUserId);
    boolean isPrivileged = isAdmin || hasManagePermissions;

    if (document.getAccessLevel() == AccessLevel.CONFIDENTIAL && !isOwner && !isPrivileged) {
      boolean hasUserGrant = currentUserId != null &&
          documentAclRepositoryPort.hasUserPermission(documentId, currentUserId, PermissionLevel.VIEW);
      if (!hasUserGrant) {
        log.warn("Access denied for user {} attempting to view ACL on confidential document {}", currentUserId, documentId);
        throw new DocumentAccessDeniedException("User does not have permission to view permissions for document " + documentId);
      }
    }

    List<DocumentUserAccess> userGrants = documentAclRepositoryPort.findUserGrants(documentId);
    List<DocumentDepartmentAccess> deptGrants = documentAclRepositoryPort.findDepartmentGrants(documentId);
    List<DocumentRoleAccess> roleGrants = documentAclRepositoryPort.findRoleGrants(documentId);

    return DocumentPermissionsResponseDto.builder()
        .documentId(document.getId())
        .accessLevel(document.getAccessLevel())
        .userGrants(toUserGrantDtos(userGrants))
        .departmentGrants(toDeptGrantDtos(deptGrants))
        .roleGrants(toRoleGrantDtos(roleGrants))
        .updatedAt(document.getUpdatedAt())
        .build();
  }

  private List<UserGrantDto> toUserGrantDtos(List<DocumentUserAccess> grants) {
    if (grants == null) return Collections.emptyList();
    return grants.stream()
        .map(g -> UserGrantDto.builder()
            .userId(g.getUserId())
            .permissionLevel(g.getPermissionLevel())
            .build())
        .collect(Collectors.toList());
  }

  private List<DepartmentGrantDto> toDeptGrantDtos(List<DocumentDepartmentAccess> grants) {
    if (grants == null) return Collections.emptyList();
    return grants.stream()
        .map(g -> DepartmentGrantDto.builder()
            .departmentId(g.getDepartmentId())
            .permissionLevel(g.getPermissionLevel())
            .build())
        .collect(Collectors.toList());
  }

  private List<RoleGrantDto> toRoleGrantDtos(List<DocumentRoleAccess> grants) {
    if (grants == null) return Collections.emptyList();
    return grants.stream()
        .map(g -> RoleGrantDto.builder()
            .roleId(g.getRoleId())
            .permissionLevel(g.getPermissionLevel())
            .build())
        .collect(Collectors.toList());
  }
}
