package com.platform.app.iam.infrastructure.adapters.primary.rest;

import com.platform.app.iam.domain.exception.AccountDisabledException;
import com.platform.app.iam.domain.exception.AccountLockedException;
import com.platform.app.iam.domain.exception.InvalidCredentialsException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class RestExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    String message =
        ex.getBindingResult().getFieldErrors().stream()
            .map(err -> err.getField() + ": " + err.getDefaultMessage())
            .collect(Collectors.joining("; "));

    ErrorResponse error =
        buildErrorResponse(HttpStatus.BAD_REQUEST, message, request);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(InvalidCredentialsException.class)
  public ResponseEntity<ErrorResponse> handleInvalidCredentials(
      InvalidCredentialsException ex, HttpServletRequest request) {
    ErrorResponse error =
        buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
  }

  @ExceptionHandler(AccountDisabledException.class)
  public ResponseEntity<ErrorResponse> handleAccountDisabled(
      AccountDisabledException ex, HttpServletRequest request) {
    ErrorResponse error =
        buildErrorResponse(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
  }

  @ExceptionHandler(AccountLockedException.class)
  public ResponseEntity<ErrorResponse> handleAccountLocked(
      AccountLockedException ex, HttpServletRequest request) {
    // 423 LOCKED
    ErrorResponse error =
        buildErrorResponse(HttpStatus.LOCKED, ex.getMessage(), request);
    return ResponseEntity.status(HttpStatus.LOCKED).body(error);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(
      Exception ex, HttpServletRequest request) {
    log.error("Unhandled exception at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
    ErrorResponse error =
        buildErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected internal error occurred", request);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }

  private ErrorResponse buildErrorResponse(
      HttpStatus status, String message, HttpServletRequest request) {
    return ErrorResponse.builder()
        .status(status.value())
        .error(status.getReasonPhrase())
        .message(message)
        .timestamp(Instant.now())
        .path(request.getRequestURI())
        .build();
  }
}
