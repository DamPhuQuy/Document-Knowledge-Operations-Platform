package com.platform.app.shared.infrastructure.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TraceIdFilterTest {

    private TraceIdFilter traceIdFilter;

    @BeforeEach
    void setUp() {
        traceIdFilter = new TraceIdFilter();
        MDC.clear();
    }

    @Test
    @DisplayName(
        "Should generate new traceId and attach to response header and MDC when missing"
    )
    void shouldGenerateTraceIdWhenHeaderMissing()
        throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.100");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> mdcTraceIdDuringExecution =
            new AtomicReference<>();
        AtomicReference<String> mdcClientIpDuringExecution =
            new AtomicReference<>();

        MockFilterChain filterChain = new MockFilterChain() {
            @Override
            public void doFilter(
                jakarta.servlet.ServletRequest req,
                jakarta.servlet.ServletResponse res
            ) {
                mdcTraceIdDuringExecution.set(
                    MDC.get(LoggingConstants.TRACE_ID_MDC_KEY)
                );
                mdcClientIpDuringExecution.set(
                    MDC.get(LoggingConstants.CLIENT_IP_MDC_KEY)
                );
            }
        };

        traceIdFilter.doFilter(request, response, filterChain);

        String responseHeader = response.getHeader(
            LoggingConstants.TRACE_ID_HEADER
        );
        assertThat(responseHeader).isNotNull().isNotBlank();
        assertThat(mdcTraceIdDuringExecution.get()).isEqualTo(responseHeader);
        assertThat(mdcClientIpDuringExecution.get()).isEqualTo("192.168.1.100");

        // MDC must be cleaned up in finally block
        assertThat(MDC.get(LoggingConstants.TRACE_ID_MDC_KEY)).isNull();
        assertThat(MDC.get(LoggingConstants.CLIENT_IP_MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("Should reuse incoming X-Trace-Id header")
    void shouldUseExistingTraceIdWhenProvided()
        throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(LoggingConstants.TRACE_ID_HEADER, "custom-trace-999");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> mdcTraceIdDuringExecution =
            new AtomicReference<>();

        MockFilterChain filterChain = new MockFilterChain() {
            @Override
            public void doFilter(
                jakarta.servlet.ServletRequest req,
                jakarta.servlet.ServletResponse res
            ) {
                mdcTraceIdDuringExecution.set(
                    MDC.get(LoggingConstants.TRACE_ID_MDC_KEY)
                );
            }
        };

        traceIdFilter.doFilter(request, response, filterChain);

        assertThat(
            response.getHeader(LoggingConstants.TRACE_ID_HEADER)
        ).isEqualTo("custom-trace-999");
        assertThat(mdcTraceIdDuringExecution.get()).isEqualTo(
            "custom-trace-999"
        );
        assertThat(MDC.get(LoggingConstants.TRACE_ID_MDC_KEY)).isNull();
    }

    @Test
    @DisplayName(
        "Should fallback to incoming X-Request-Id header if X-Trace-Id is absent"
    )
    void shouldFallbackToRequestIdWhenTraceIdMissing()
        throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(
            LoggingConstants.REQUEST_ID_HEADER,
            "gateway-req-555"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> mdcTraceIdDuringExecution =
            new AtomicReference<>();

        MockFilterChain filterChain = new MockFilterChain() {
            @Override
            public void doFilter(
                jakarta.servlet.ServletRequest req,
                jakarta.servlet.ServletResponse res
            ) {
                mdcTraceIdDuringExecution.set(
                    MDC.get(LoggingConstants.TRACE_ID_MDC_KEY)
                );
            }
        };

        traceIdFilter.doFilter(request, response, filterChain);

        assertThat(
            response.getHeader(LoggingConstants.TRACE_ID_HEADER)
        ).isEqualTo("gateway-req-555");
        assertThat(mdcTraceIdDuringExecution.get()).isEqualTo(
            "gateway-req-555"
        );
        assertThat(MDC.get(LoggingConstants.TRACE_ID_MDC_KEY)).isNull();
    }

    @Test
    @DisplayName(
        "Should guarantee MDC cleanup even when downstream filter chain throws exception"
    )
    void shouldCleanUpMdcEvenWhenFilterChainThrows() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        MockFilterChain filterChain = new MockFilterChain() {
            @Override
            public void doFilter(
                jakarta.servlet.ServletRequest req,
                jakarta.servlet.ServletResponse res
            ) {
                throw new IllegalStateException("Simulated downstream error");
            }
        };

        assertThatThrownBy(() ->
            traceIdFilter.doFilter(request, response, filterChain)
        )
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Simulated downstream error");

        // MDC must remain clean despite exception
        assertThat(MDC.get(LoggingConstants.TRACE_ID_MDC_KEY)).isNull();
        assertThat(MDC.get(LoggingConstants.CLIENT_IP_MDC_KEY)).isNull();
    }
}
