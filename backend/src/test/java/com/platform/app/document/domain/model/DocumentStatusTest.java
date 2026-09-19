package com.platform.app.document.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DocumentStatusTest {

    @Test
    @DisplayName("Should verify all DocumentStatus enum constants")
    void shouldVerifyEnumConstants() {
        assertThat(DocumentStatus.values()).containsExactly(
            DocumentStatus.UPLOADED,
            DocumentStatus.PROCESSING,
            DocumentStatus.READY,
            DocumentStatus.FAILED
        );
        assertThat(DocumentStatus.valueOf("UPLOADED")).isEqualTo(DocumentStatus.UPLOADED);
        assertThat(DocumentStatus.valueOf("PROCESSING")).isEqualTo(DocumentStatus.PROCESSING);
        assertThat(DocumentStatus.valueOf("READY")).isEqualTo(DocumentStatus.READY);
        assertThat(DocumentStatus.valueOf("FAILED")).isEqualTo(DocumentStatus.FAILED);
    }
}
