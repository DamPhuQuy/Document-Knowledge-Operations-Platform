package com.platform.app.shared.infrastructure.logging;

public final class LoggingConstants {

  private LoggingConstants() {}

  public static final String TRACE_ID_MDC_KEY = "traceId";
  public static final String CLIENT_IP_MDC_KEY = "clientIp";
  public static final String TRACE_ID_HEADER = "X-Trace-Id";
  public static final String REQUEST_ID_HEADER = "X-Request-Id";
}
