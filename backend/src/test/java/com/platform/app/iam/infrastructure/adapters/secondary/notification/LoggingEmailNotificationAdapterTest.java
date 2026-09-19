package com.platform.app.iam.infrastructure.adapters.secondary.notification;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LoggingEmailNotificationAdapterTest {

    @Test
    @DisplayName("Should log and complete sendOtpEmail without throwing exceptions")
    void shouldCompleteSendOtpEmailWithoutException() {
        LoggingEmailNotificationAdapter adapter = new LoggingEmailNotificationAdapter();

        assertThatCode(() -> adapter.sendOtpEmail("user@example.com", "123456", "John Doe"))
            .doesNotThrowAnyException();
    }
}
