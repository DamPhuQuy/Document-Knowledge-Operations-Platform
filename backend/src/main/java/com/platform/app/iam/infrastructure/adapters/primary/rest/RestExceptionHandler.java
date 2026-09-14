package com.platform.app.iam.infrastructure.adapters.primary.rest;

import com.platform.app.iam.domain.exception.AccountDisabledException;
import com.platform.app.iam.domain.exception.AccountLockedException;
import com.platform.app.iam.domain.exception.DepartmentCodeConflictException;
import com.platform.app.iam.domain.exception.DepartmentNotFoundException;
import com.platform.app.iam.domain.exception.EmptyRolesException;
import com.platform.app.iam.domain.exception.InvalidCredentialsException;
import com.platform.app.iam.domain.exception.InvalidDepartmentCodeException;
import com.platform.app.iam.domain.exception.SelfRoleRevocationException;
import com.platform.app.iam.infrastructure.adapters.primary.rest.dto.response.ErrorResponse;

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

    log.warn("Validation failed on {}: {}", request.getRequestURI(), message);
    ErrorResponse error =
        buildErrorResponse(HttpStatus.BAD_REQUEST, message, request);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(InvalidCredentialsException.class)
  public ResponseEntity<ErrorResponse> handleInvalidCredentials(
      InvalidCredentialsException ex, HttpServletRequest request) {
    log.warn("Authentication failed on {}: {}", request.getRequestURI(), ex.getMessage());
    ErrorResponse error =
        buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
  }

  @ExceptionHandler(AccountDisabledException.class)
  public ResponseEntity<ErrorResponse> handleAccountDisabled(
      AccountDisabledException ex, HttpServletRequest request) {
    log.warn("Account disabled rejection on {}: {}", request.getRequestURI(), ex.getMessage());
    ErrorResponse error =
        buildErrorResponse(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
  }

  @ExceptionHandler(AccountLockedException.class)
  public ResponseEntity<ErrorResponse> handleAccountLocked(
      AccountLockedException ex, HttpServletRequest request) {
    // 423 LOCKED
    log.warn("Account locked rejection on {}: {}", request.getRequestURI(), ex.getMessage());
    ErrorResponse error =
        buildErrorResponse(HttpStatus.LOCKED, ex.getMessage(), request);
    return ResponseEntity.status(HttpStatus.LOCKED).body(error);
  }

  @ExceptionHandler(DepartmentNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleDepartmentNotFound(
      DepartmentNotFoundException ex, HttpServletRequest request) {
    log.warn("Resource not found on {}: {}", request.getRequestURI(), ex.getMessage());
    ErrorResponse error =
        buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
  }

  @ExceptionHandler(DepartmentCodeConflictException.class)
  public ResponseEntity<ErrorResponse> handleDepartmentCodeConflict(
      DepartmentCodeConflictException ex, HttpServletRequest request) {
    log.warn("Resource conflict on {}: {}", request.getRequestURI(), ex.getMessage());
    ErrorResponse error =
        buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
    return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
  }

  @ExceptionHandler({
    EmptyRolesException.class,
    SelfRoleRevocationException.class,
    InvalidDepartmentCodeException.class,
    IllegalArgumentException.class
  })
  public ResponseEntity<ErrorResponse> handleBadRequestExceptions(
      RuntimeException ex, HttpServletRequest request) {
    log.warn("Bad request rejection on {}: {}", request.getRequestURI(), ex.getMessage());
    ErrorResponse error =
        buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDenied(
      org.springframework.security.access.AccessDeniedException ex, HttpServletRequest request) {
    log.warn("Access denied on {}: {}", request.getRequestURI(), ex.getMessage());
    ErrorResponse error =
        buildErrorResponse(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
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
