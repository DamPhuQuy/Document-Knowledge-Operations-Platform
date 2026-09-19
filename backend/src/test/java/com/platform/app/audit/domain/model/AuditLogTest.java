package com.platform.app.audit.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuditLogTest {

  @Test
  @DisplayName("Should create immutable AuditLog with default status SUCCESS and empty details")
  void shouldCreateAuditLogWithDefaults() {
    UUID id = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Instant now = Instant.now();

    AuditLog log =
        AuditLog.builder()
            .id(id)
            .userId(userId)
            .action("LOGIN")
            .resourceType("USER")
            .createdAt(now)
            .build();

    assertThat(log.getId()).isEqualTo(id);
    assertThat(log.getUserId()).isEqualTo(userId);
    assertThat(log.getAction()).isEqualTo("LOGIN");
    assertThat(log.getResourceType()).isEqualTo("USER");
    assertThat(log.getStatus()).isEqualTo(AuditStatus.SUCCESS);
    assertThat(log.getDetails()).isEmpty();
    assertThat(log.getCreatedAt()).isEqualTo(now);
  }

  @Test
  @DisplayName("Should reject null action or null resourceType")
  void shouldRejectNullActionOrResource() {
    assertThatThrownBy(() -> AuditLog.builder().resourceType("USER").build())
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("Audit action must not be null");

    assertThatThrownBy(() -> AuditLog.builder().action("LOGIN").build())
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("Resource type must not be null");
  }

  @Test
  @DisplayName("Should ensure details map is unmodifiable")
  void shouldEnsureDetailsIsUnmodifiable() {
    Map<String, Object> details = Map.of("key", "value");
    AuditLog log =
        AuditLog.builder()
            .action("UPLOAD_DOC")
            .resourceType("DOCUMENT")
            .details(details)
            .build();

    assertThat(log.getDetails()).containsEntry("key", "value");
    assertThatThrownBy(() -> log.getDetails().put("newKey", "newValue"))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
