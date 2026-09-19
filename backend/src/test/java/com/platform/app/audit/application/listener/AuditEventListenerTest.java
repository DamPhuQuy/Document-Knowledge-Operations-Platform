package com.platform.app.audit.application.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.platform.app.audit.application.dto.RecordAuditLogCommand;
import com.platform.app.audit.application.ports.inbound.RecordAuditLogUseCase;
import com.platform.app.audit.domain.model.AuditStatus;
import com.platform.app.document.application.event.DocumentAclUpdatedEvent;
import com.platform.app.document.application.event.DocumentSoftDeletedEvent;
import com.platform.app.document.application.event.DocumentUploadedEvent;
import com.platform.app.document.application.event.DocumentVersionCreatedEvent;
import com.platform.app.document.domain.model.AccessLevel;
import com.platform.app.iam.application.dto.DepartmentCreatedEvent;
import com.platform.app.iam.application.dto.UserLoginFailedEvent;
import com.platform.app.iam.application.dto.UserLoginSuccessEvent;
import com.platform.app.iam.application.dto.UserRolesUpdatedEvent;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditEventListenerTest {

    @Mock
    private RecordAuditLogUseCase recordAuditLogUseCase;

    private AuditEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new AuditEventListener(recordAuditLogUseCase);
    }

    @Test
    @DisplayName("Should handle DocumentUploadedEvent")
    void shouldHandleDocumentUploaded() {
        UUID docId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        DocumentUploadedEvent event = DocumentUploadedEvent.builder()
            .documentId(docId)
            .title("Test Doc")
            .originalFileName("test.pdf")
            .contentType("application/pdf")
            .fileSizeBytes(1024L)
            .uploadedByUserId(userId)
            .timestamp(now)
            .build();

        listener.handleDocumentUploaded(event);

        ArgumentCaptor<RecordAuditLogCommand> captor = ArgumentCaptor.forClass(
            RecordAuditLogCommand.class
        );
        verify(recordAuditLogUseCase).recordAuditLog(captor.capture());

        RecordAuditLogCommand cmd = captor.getValue();
        assertThat(cmd.getAction()).isEqualTo("UPLOAD_DOC");
        assertThat(cmd.getResourceType()).isEqualTo("DOCUMENT");
        assertThat(cmd.getResourceId()).isEqualTo(docId.toString());
        assertThat(cmd.getUserId()).isEqualTo(userId);
        assertThat(cmd.getStatus()).isEqualTo(AuditStatus.SUCCESS);
        assertThat(cmd.getDetails()).containsEntry("title", "Test Doc");
    }

    @Test
    @DisplayName("Should handle DocumentVersionCreatedEvent")
    void shouldHandleDocumentVersionCreated() {
        UUID docId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        DocumentVersionCreatedEvent event =
            DocumentVersionCreatedEvent.builder()
                .documentId(docId)
                .versionId(versionId)
                .versionNumber(2)
                .fileSizeBytes(2048L)
                .uploadedByUserId(userId)
                .timestamp(Instant.now())
                .build();

        listener.handleDocumentVersionCreated(event);

        ArgumentCaptor<RecordAuditLogCommand> captor = ArgumentCaptor.forClass(
            RecordAuditLogCommand.class
        );
        verify(recordAuditLogUseCase).recordAuditLog(captor.capture());

        RecordAuditLogCommand cmd = captor.getValue();
        assertThat(cmd.getAction()).isEqualTo("CREATE_VERSION");
        assertThat(cmd.getResourceId()).isEqualTo(docId.toString());
        assertThat(cmd.getDetails()).containsEntry("versionNumber", 2);
    }

    @Test
    @DisplayName("Should handle DocumentAclUpdatedEvent")
    void shouldHandleDocumentAclUpdated() {
        UUID docId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        DocumentAclUpdatedEvent event = DocumentAclUpdatedEvent.builder()
            .documentId(docId)
            .updatedByUserId(userId)
            .accessLevel(AccessLevel.RESTRICTED)
            .userGrantsCount(1)
            .departmentGrantsCount(2)
            .roleGrantsCount(0)
            .timestamp(Instant.now())
            .build();

        listener.handleDocumentAclUpdated(event);

        ArgumentCaptor<RecordAuditLogCommand> captor = ArgumentCaptor.forClass(
            RecordAuditLogCommand.class
        );
        verify(recordAuditLogUseCase).recordAuditLog(captor.capture());

        RecordAuditLogCommand cmd = captor.getValue();
        assertThat(cmd.getAction()).isEqualTo("UPDATE_ACL");
        assertThat(cmd.getDetails()).containsEntry("accessLevel", "RESTRICTED");
    }

    @Test
    @DisplayName("Should handle DocumentSoftDeletedEvent")
    void shouldHandleDocumentSoftDeleted() {
        UUID docId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        DocumentSoftDeletedEvent event = DocumentSoftDeletedEvent.builder()
            .documentId(docId)
            .deletedByUserId(userId)
            .timestamp(Instant.now())
            .build();

        listener.handleDocumentSoftDeleted(event);

        ArgumentCaptor<RecordAuditLogCommand> captor = ArgumentCaptor.forClass(
            RecordAuditLogCommand.class
        );
        verify(recordAuditLogUseCase).recordAuditLog(captor.capture());

        RecordAuditLogCommand cmd = captor.getValue();
        assertThat(cmd.getAction()).isEqualTo("DELETE_DOC");
        assertThat(cmd.getResourceId()).isEqualTo(docId.toString());
    }

    @Test
    @DisplayName("Should handle UserLoginSuccessEvent")
    void shouldHandleUserLoginSuccess() {
        UUID userId = UUID.randomUUID();
        UserLoginSuccessEvent event = UserLoginSuccessEvent.builder()
            .userId(userId)
            .email("test@example.com")
            .clientIp("127.0.0.1")
            .userAgent("Mozilla")
            .timestamp(Instant.now())
            .build();

        listener.handleUserLoginSuccess(event);

        ArgumentCaptor<RecordAuditLogCommand> captor = ArgumentCaptor.forClass(
            RecordAuditLogCommand.class
        );
        verify(recordAuditLogUseCase).recordAuditLog(captor.capture());

        RecordAuditLogCommand cmd = captor.getValue();
        assertThat(cmd.getAction()).isEqualTo("LOGIN");
        assertThat(cmd.getUserId()).isEqualTo(userId);
        assertThat(cmd.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(cmd.getStatus()).isEqualTo(AuditStatus.SUCCESS);
    }

    @Test
    @DisplayName("Should handle UserLoginFailedEvent")
    void shouldHandleUserLoginFailed() {
        UserLoginFailedEvent event = UserLoginFailedEvent.builder()
            .email("bad@example.com")
            .clientIp("10.0.0.1")
            .userAgent("Curl")
            .reason("Invalid credentials")
            .timestamp(Instant.now())
            .build();

        listener.handleUserLoginFailed(event);

        ArgumentCaptor<RecordAuditLogCommand> captor = ArgumentCaptor.forClass(
            RecordAuditLogCommand.class
        );
        verify(recordAuditLogUseCase).recordAuditLog(captor.capture());

        RecordAuditLogCommand cmd = captor.getValue();
        assertThat(cmd.getAction()).isEqualTo("LOGIN_FAILED");
        assertThat(cmd.getStatus()).isEqualTo(AuditStatus.FAILED);
        assertThat(cmd.getDetails()).containsEntry(
            "reason",
            "Invalid credentials"
        );
    }

    @Test
    @DisplayName("Should handle UserRolesUpdatedEvent")
    void shouldHandleUserRolesUpdated() {
        UUID targetUserId = UUID.randomUUID();
        UUID operatorId = UUID.randomUUID();
        UserRolesUpdatedEvent event = new UserRolesUpdatedEvent(
            targetUserId,
            Set.of("STAFF"),
            Set.of("MANAGER"),
            operatorId,
            Instant.now()
        );

        listener.handleUserRolesUpdated(event);

        ArgumentCaptor<RecordAuditLogCommand> captor = ArgumentCaptor.forClass(
            RecordAuditLogCommand.class
        );
        verify(recordAuditLogUseCase).recordAuditLog(captor.capture());

        RecordAuditLogCommand cmd = captor.getValue();
        assertThat(cmd.getAction()).isEqualTo("ASSIGN_ROLES");
        assertThat(cmd.getUserId()).isEqualTo(operatorId);
        assertThat(cmd.getResourceId()).isEqualTo(targetUserId.toString());
    }

    @Test
    @DisplayName("Should handle DepartmentCreatedEvent")
    void shouldHandleDepartmentCreated() {
        UUID deptId = UUID.randomUUID();
        UUID operatorId = UUID.randomUUID();
        DepartmentCreatedEvent event = DepartmentCreatedEvent.builder()
            .departmentId(deptId)
            .code("HR")
            .name("Human Resources")
            .operatorUserId(operatorId)
            .timestamp(Instant.now())
            .build();

        listener.handleDepartmentCreated(event);

        ArgumentCaptor<RecordAuditLogCommand> captor = ArgumentCaptor.forClass(
            RecordAuditLogCommand.class
        );
        verify(recordAuditLogUseCase).recordAuditLog(captor.capture());

        RecordAuditLogCommand cmd = captor.getValue();
        assertThat(cmd.getAction()).isEqualTo("CREATE_DEPARTMENT");
        assertThat(cmd.getResourceType()).isEqualTo("DEPARTMENT");
        assertThat(cmd.getResourceId()).isEqualTo(deptId.toString());
    }
}
