package com.platform.app.audit.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.platform.app.audit.application.dto.AuditLogQueryFilter;
import com.platform.app.audit.application.dto.AuditLogResponseDto;
import com.platform.app.audit.application.dto.RecordAuditLogCommand;
import com.platform.app.audit.application.ports.outbound.AuditLogRepositoryPort;
import com.platform.app.audit.domain.model.AuditLog;
import com.platform.app.audit.domain.model.AuditStatus;

@ExtendWith(MockitoExtension.class)
class AuditLoggingServiceTest {

  @Mock private AuditLogRepositoryPort auditLogRepositoryPort;

  private AuditLoggingService service;

  @BeforeEach
  void setUp() {
    service = new AuditLoggingService(auditLogRepositoryPort);
  }

  @Test
  @DisplayName("Should record audit log command and persist via repository port")
  void shouldRecordAuditLog() {
    UUID userId = UUID.randomUUID();
    Instant now = Instant.now();
    RecordAuditLogCommand command =
        RecordAuditLogCommand.builder()
            .userId(userId)
            .action("UPLOAD_DOC")
            .resourceType("DOCUMENT")
            .resourceId("doc-123")
            .ipAddress("192.168.1.1")
            .userAgent("Safari")
            .status(AuditStatus.SUCCESS)
            .details(Map.of("fileName", "contract.pdf"))
            .timestamp(now)
            .build();

    when(auditLogRepositoryPort.save(any(AuditLog.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    AuditLog result = service.recordAuditLog(command);

    assertThat(result).isNotNull();
    assertThat(result.getAction()).isEqualTo("UPLOAD_DOC");
    assertThat(result.getResourceType()).isEqualTo("DOCUMENT");
    assertThat(result.getUserId()).isEqualTo(userId);
    assertThat(result.getDetails()).containsEntry("fileName", "contract.pdf");

    ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
    verify(auditLogRepositoryPort).save(captor.capture());
    assertThat(captor.getValue().getResourceId()).isEqualTo("doc-123");
  }

  @Test
  @DisplayName("Should query audit logs with filter and map to response DTOs")
  void shouldGetAuditLogs() {
    UUID id = UUID.randomUUID();
    AuditLog log =
        AuditLog.builder()
            .id(id)
            .action("DELETE_DOC")
            .resourceType("DOCUMENT")
            .status(AuditStatus.SUCCESS)
            .createdAt(Instant.now())
            .build();

    Page<AuditLog> page = new PageImpl<>(List.of(log));
    AuditLogQueryFilter filter = AuditLogQueryFilter.builder().action("DELETE_DOC").build();
    Pageable pageable = PageRequest.of(0, 10);

    when(auditLogRepositoryPort.findAll(filter, pageable)).thenReturn(page);

    Page<AuditLogResponseDto> result = service.getAuditLogs(filter, pageable);

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getId()).isEqualTo(id);
    assertThat(result.getContent().get(0).getAction()).isEqualTo("DELETE_DOC");
  }
}
