package com.platform.app.audit.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuditStatusTest {

    @Test
    @DisplayName("Should verify AuditStatus enum values")
    void shouldVerifyEnumValues() {
        assertThat(AuditStatus.values()).containsExactly(
            AuditStatus.SUCCESS,
            AuditStatus.FAILED
        );
        assertThat(AuditStatus.valueOf("SUCCESS")).isEqualTo(AuditStatus.SUCCESS);
        assertThat(AuditStatus.valueOf("FAILED")).isEqualTo(AuditStatus.FAILED);
    }
}
