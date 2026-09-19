package com.platform.app.shared.infrastructure.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LoggingConstantsTest {

    @Test
    @DisplayName("Should verify logging constant values")
    void shouldVerifyLoggingConstants() {
        assertThat(LoggingConstants.TRACE_ID_MDC_KEY).isEqualTo("traceId");
        assertThat(LoggingConstants.CLIENT_IP_MDC_KEY).isEqualTo("clientIp");
        assertThat(LoggingConstants.TRACE_ID_HEADER).isEqualTo("X-Trace-Id");
        assertThat(LoggingConstants.REQUEST_ID_HEADER).isEqualTo("X-Request-Id");
    }
}
