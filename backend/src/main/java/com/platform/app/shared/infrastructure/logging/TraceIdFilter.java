package com.platform.app.shared.infrastructure.logging;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain)
      throws ServletException, IOException {

    String traceId = resolveTraceId(request);
    String clientIp = resolveClientIp(request);

    try {
      MDC.put(LoggingConstants.TRACE_ID_MDC_KEY, traceId);
      MDC.put(LoggingConstants.CLIENT_IP_MDC_KEY, clientIp);
      response.setHeader(LoggingConstants.TRACE_ID_HEADER, traceId);

      filterChain.doFilter(request, response);
    } finally {
      MDC.remove(LoggingConstants.TRACE_ID_MDC_KEY);
      MDC.remove(LoggingConstants.CLIENT_IP_MDC_KEY);
    }
  }

  private String resolveTraceId(HttpServletRequest request) {
    String traceId = request.getHeader(LoggingConstants.TRACE_ID_HEADER);
    if (traceId != null && !traceId.isBlank()) {
      return traceId.trim();
    }
    String requestId = request.getHeader(LoggingConstants.REQUEST_ID_HEADER);
    if (requestId != null && !requestId.isBlank()) {
      return requestId.trim();
    }
    return UUID.randomUUID().toString();
  }

  private String resolveClientIp(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isBlank()) {
      return xForwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
  }
}
