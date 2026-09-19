package com.platform.app.audit.application.listener;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.platform.app.audit.application.dto.RecordAuditLogCommand;
import com.platform.app.audit.application.ports.inbound.RecordAuditLogUseCase;
import com.platform.app.audit.domain.model.AuditStatus;
import com.platform.app.document.application.event.DocumentAclUpdatedEvent;
import com.platform.app.document.application.event.DocumentSoftDeletedEvent;
import com.platform.app.document.application.event.DocumentUploadedEvent;
import com.platform.app.document.application.event.DocumentVersionCreatedEvent;
import com.platform.app.iam.application.dto.DepartmentCreatedEvent;
import com.platform.app.iam.application.dto.DepartmentUpdatedEvent;
import com.platform.app.iam.application.dto.UserDepartmentAssignedEvent;
import com.platform.app.iam.application.dto.UserLoginFailedEvent;
import com.platform.app.iam.application.dto.UserLoginSuccessEvent;
import com.platform.app.iam.application.dto.UserRegisteredOtpEvent;
import com.platform.app.iam.application.dto.UserRolesUpdatedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEventListener {

  private final RecordAuditLogUseCase recordAuditLogUseCase;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void handleDocumentUploaded(DocumentUploadedEvent event) {
    if (event == null) return;
    log.debug("Auditing DocumentUploadedEvent for docId={}", event.getDocumentId());

    Map<String, Object> details = new HashMap<>();
    if (event.getTitle() != null) details.put("title", event.getTitle());
    if (event.getOriginalFileName() != null) details.put("originalFileName", event.getOriginalFileName());
    details.put("fileSizeBytes", event.getFileSizeBytes());
    if (event.getContentType() != null) details.put("contentType", event.getContentType());
    if (event.getDepartmentId() != null) details.put("departmentId", event.getDepartmentId().toString());

    recordAuditLogUseCase.recordAuditLog(
        RecordAuditLogCommand.builder()
            .userId(event.getUploadedByUserId())
            .action("UPLOAD_DOC")
            .resourceType("DOCUMENT")
            .resourceId(event.getDocumentId() != null ? event.getDocumentId().toString() : null)
            .status(AuditStatus.SUCCESS)
            .details(details)
            .timestamp(event.getTimestamp())
            .build());
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void handleDocumentVersionCreated(DocumentVersionCreatedEvent event) {
    if (event == null) return;
    log.debug("Auditing DocumentVersionCreatedEvent for docId={}", event.getDocumentId());

    Map<String, Object> details = new HashMap<>();
    details.put("versionNumber", event.getVersionNumber());
    if (event.getVersionId() != null) details.put("versionId", event.getVersionId().toString());
    details.put("fileSizeBytes", event.getFileSizeBytes());

    recordAuditLogUseCase.recordAuditLog(
        RecordAuditLogCommand.builder()
            .userId(event.getUploadedByUserId())
            .action("CREATE_VERSION")
            .resourceType("DOCUMENT")
            .resourceId(event.getDocumentId() != null ? event.getDocumentId().toString() : null)
            .status(AuditStatus.SUCCESS)
            .details(details)
            .timestamp(event.getTimestamp())
            .build());
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void handleDocumentAclUpdated(DocumentAclUpdatedEvent event) {
    if (event == null) return;
    log.debug("Auditing DocumentAclUpdatedEvent for docId={}", event.getDocumentId());

    Map<String, Object> details = new HashMap<>();
    if (event.getAccessLevel() != null) details.put("accessLevel", event.getAccessLevel().name());
    details.put("userGrantsCount", event.getUserGrantsCount());
    details.put("departmentGrantsCount", event.getDepartmentGrantsCount());
    details.put("roleGrantsCount", event.getRoleGrantsCount());

    recordAuditLogUseCase.recordAuditLog(
        RecordAuditLogCommand.builder()
            .userId(event.getUpdatedByUserId())
            .action("UPDATE_ACL")
            .resourceType("DOCUMENT")
            .resourceId(event.getDocumentId() != null ? event.getDocumentId().toString() : null)
            .status(AuditStatus.SUCCESS)
            .details(details)
            .timestamp(event.getTimestamp())
            .build());
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void handleDocumentSoftDeleted(DocumentSoftDeletedEvent event) {
    if (event == null) return;
    log.debug("Auditing DocumentSoftDeletedEvent for docId={}", event.getDocumentId());

    recordAuditLogUseCase.recordAuditLog(
        RecordAuditLogCommand.builder()
            .userId(event.getDeletedByUserId())
            .action("DELETE_DOC")
            .resourceType("DOCUMENT")
            .resourceId(event.getDocumentId() != null ? event.getDocumentId().toString() : null)
            .status(AuditStatus.SUCCESS)
            .details(Collections.emptyMap())
            .timestamp(event.getTimestamp())
            .build());
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void handleUserLoginSuccess(UserLoginSuccessEvent event) {
    if (event == null) return;
    log.debug("Auditing UserLoginSuccessEvent for userId={}", event.userId());

    Map<String, Object> details = new HashMap<>();
    if (event.email() != null) details.put("email", event.email());

    recordAuditLogUseCase.recordAuditLog(
        RecordAuditLogCommand.builder()
            .userId(event.userId())
            .action("LOGIN")
            .resourceType("USER")
            .resourceId(event.userId() != null ? event.userId().toString() : null)
            .ipAddress(event.clientIp())
            .userAgent(event.userAgent())
            .status(AuditStatus.SUCCESS)
            .details(details)
            .timestamp(event.timestamp())
            .build());
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void handleUserLoginFailed(UserLoginFailedEvent event) {
    if (event == null) return;
    log.debug("Auditing UserLoginFailedEvent for email={}", event.email());

    Map<String, Object> details = new HashMap<>();
    if (event.email() != null) details.put("email", event.email());
    if (event.reason() != null) details.put("reason", event.reason());

    recordAuditLogUseCase.recordAuditLog(
        RecordAuditLogCommand.builder()
            .userId(null)
            .action("LOGIN_FAILED")
            .resourceType("USER")
            .resourceId(null)
            .ipAddress(event.clientIp())
            .userAgent(event.userAgent())
            .status(AuditStatus.FAILED)
            .details(details)
            .timestamp(event.timestamp())
            .build());
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void handleUserRolesUpdated(UserRolesUpdatedEvent event) {
    if (event == null) return;
    log.debug("Auditing UserRolesUpdatedEvent for targetUserId={}", event.targetUserId());

    Map<String, Object> details = new HashMap<>();
    if (event.targetUserId() != null) details.put("targetUserId", event.targetUserId().toString());
    if (event.oldRoleCodes() != null) details.put("oldRoles", event.oldRoleCodes());
    if (event.newRoleCodes() != null) details.put("newRoles", event.newRoleCodes());

    recordAuditLogUseCase.recordAuditLog(
        RecordAuditLogCommand.builder()
            .userId(event.operatorUserId())
            .action("ASSIGN_ROLES")
            .resourceType("USER")
            .resourceId(event.targetUserId() != null ? event.targetUserId().toString() : null)
            .status(AuditStatus.SUCCESS)
            .details(details)
            .timestamp(event.timestamp())
            .build());
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void handleDepartmentCreated(DepartmentCreatedEvent event) {
    if (event == null) return;
    log.debug("Auditing DepartmentCreatedEvent for departmentId={}", event.departmentId());

    Map<String, Object> details = new HashMap<>();
    if (event.code() != null) details.put("code", event.code());
    if (event.name() != null) details.put("name", event.name());

    recordAuditLogUseCase.recordAuditLog(
        RecordAuditLogCommand.builder()
            .userId(event.operatorUserId())
            .action("CREATE_DEPARTMENT")
            .resourceType("DEPARTMENT")
            .resourceId(event.departmentId() != null ? event.departmentId().toString() : null)
            .status(AuditStatus.SUCCESS)
            .details(details)
            .timestamp(event.timestamp())
            .build());
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void handleDepartmentUpdated(DepartmentUpdatedEvent event) {
    if (event == null) return;
    log.debug("Auditing DepartmentUpdatedEvent for departmentId={}", event.departmentId());

    Map<String, Object> details = new HashMap<>();
    if (event.code() != null) details.put("code", event.code());
    if (event.name() != null) details.put("name", event.name());

    recordAuditLogUseCase.recordAuditLog(
        RecordAuditLogCommand.builder()
            .userId(event.operatorUserId())
            .action("UPDATE_DEPARTMENT")
            .resourceType("DEPARTMENT")
            .resourceId(event.departmentId() != null ? event.departmentId().toString() : null)
            .status(AuditStatus.SUCCESS)
            .details(details)
            .timestamp(event.timestamp())
            .build());
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void handleUserDepartmentAssigned(UserDepartmentAssignedEvent event) {
    if (event == null) return;
    log.debug("Auditing UserDepartmentAssignedEvent for targetUserId={}", event.targetUserId());

    Map<String, Object> details = new HashMap<>();
    if (event.departmentId() != null) details.put("departmentId", event.departmentId().toString());
    details.put("internal", event.internal());

    recordAuditLogUseCase.recordAuditLog(
        RecordAuditLogCommand.builder()
            .userId(event.operatorUserId())
            .action("ASSIGN_DEPARTMENT")
            .resourceType("USER")
            .resourceId(event.targetUserId() != null ? event.targetUserId().toString() : null)
            .status(AuditStatus.SUCCESS)
            .details(details)
            .timestamp(event.timestamp())
            .build());
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void handleUserRegisteredOtp(UserRegisteredOtpEvent event) {
    if (event == null) return;
    log.debug("Auditing UserRegisteredOtpEvent for email={}", event.email());

    Map<String, Object> details = new HashMap<>();
    if (event.email() != null) details.put("email", event.email());
    if (event.fullName() != null) details.put("fullName", event.fullName());

    recordAuditLogUseCase.recordAuditLog(
        RecordAuditLogCommand.builder()
            .userId(null)
            .action("REGISTER_OTP")
            .resourceType("USER")
            .resourceId(event.email())
            .status(AuditStatus.SUCCESS)
            .details(details)
            .timestamp(java.time.Instant.now())
            .build());
  }
}
