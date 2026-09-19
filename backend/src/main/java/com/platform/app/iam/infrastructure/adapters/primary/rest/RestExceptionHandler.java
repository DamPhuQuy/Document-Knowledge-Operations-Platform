package com.platform.app.iam.infrastructure.adapters.primary.rest;

import com.platform.app.iam.domain.exception.AccountDisabledException;
import com.platform.app.iam.domain.exception.AccountLockedException;
import com.platform.app.iam.domain.exception.DepartmentCodeConflictException;
import com.platform.app.iam.domain.exception.DepartmentNotFoundException;
import com.platform.app.iam.domain.exception.EmailAlreadyExistsException;
import com.platform.app.iam.domain.exception.EmptyRolesException;
import com.platform.app.iam.domain.exception.InvalidCredentialsException;
import com.platform.app.iam.domain.exception.InvalidDepartmentCodeException;
import com.platform.app.iam.domain.exception.InvalidOtpException;
import com.platform.app.iam.domain.exception.OtpExpiredException;
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
        MethodArgumentNotValidException ex,
        HttpServletRequest request
    ) {
        String message = ex
            .getBindingResult()
            .getFieldErrors()
            .stream()
            .map(err -> err.getField() + ": " + err.getDefaultMessage())
            .collect(Collectors.joining("; "));

        log.warn(
            "Validation failed on {}: {}",
            request.getRequestURI(),
            message
        );
        ErrorResponse error = buildErrorResponse(
            HttpStatus.BAD_REQUEST,
            message,
            request
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(
        InvalidCredentialsException ex,
        HttpServletRequest request
    ) {
        log.warn(
            "Authentication failed on {}: {}",
            request.getRequestURI(),
            ex.getMessage()
        );
        ErrorResponse error = buildErrorResponse(
            HttpStatus.UNAUTHORIZED,
            ex.getMessage(),
            request
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(AccountDisabledException.class)
    public ResponseEntity<ErrorResponse> handleAccountDisabled(
        AccountDisabledException ex,
        HttpServletRequest request
    ) {
        log.warn(
            "Account disabled rejection on {}: {}",
            request.getRequestURI(),
            ex.getMessage()
        );
        ErrorResponse error = buildErrorResponse(
            HttpStatus.FORBIDDEN,
            ex.getMessage(),
            request
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ErrorResponse> handleAccountLocked(
        AccountLockedException ex,
        HttpServletRequest request
    ) {
        // 423 LOCKED
        log.warn(
            "Account locked rejection on {}: {}",
            request.getRequestURI(),
            ex.getMessage()
        );
        ErrorResponse error = buildErrorResponse(
            HttpStatus.LOCKED,
            ex.getMessage(),
            request
        );
        return ResponseEntity.status(HttpStatus.LOCKED).body(error);
    }

    @ExceptionHandler(DepartmentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleDepartmentNotFound(
        DepartmentNotFoundException ex,
        HttpServletRequest request
    ) {
        log.warn(
            "Resource not found on {}: {}",
            request.getRequestURI(),
            ex.getMessage()
        );
        ErrorResponse error = buildErrorResponse(
            HttpStatus.NOT_FOUND,
            ex.getMessage(),
            request
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(DepartmentCodeConflictException.class)
    public ResponseEntity<ErrorResponse> handleDepartmentCodeConflict(
        DepartmentCodeConflictException ex,
        HttpServletRequest request
    ) {
        log.warn(
            "Resource conflict on {}: {}",
            request.getRequestURI(),
            ex.getMessage()
        );
        ErrorResponse error = buildErrorResponse(
            HttpStatus.CONFLICT,
            ex.getMessage(),
            request
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(
        EmailAlreadyExistsException ex,
        HttpServletRequest request
    ) {
        log.warn(
            "Email conflict on {}: {}",
            request.getRequestURI(),
            ex.getMessage()
        );
        ErrorResponse error = buildErrorResponse(
            HttpStatus.CONFLICT,
            ex.getMessage(),
            request
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler({ InvalidOtpException.class, OtpExpiredException.class })
    public ResponseEntity<ErrorResponse> handleOtpException(
        RuntimeException ex,
        HttpServletRequest request
    ) {
        log.warn(
            "OTP verification error on {}: {}",
            request.getRequestURI(),
            ex.getMessage()
        );
        ErrorResponse error = buildErrorResponse(
            HttpStatus.BAD_REQUEST,
            ex.getMessage(),
            request
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(
        com.platform.app.document.domain.exception.UnsupportedMediaTypeException.class
    )
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(
        com.platform.app.document.domain.exception.UnsupportedMediaTypeException ex,
        HttpServletRequest request
    ) {
        log.warn(
            "Unsupported media type on {}: {}",
            request.getRequestURI(),
            ex.getMessage()
        );
        ErrorResponse error = buildErrorResponse(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            ex.getMessage(),
            request
        );
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(
            error
        );
    }

    @ExceptionHandler({
        com.platform.app.document.domain.exception.PayloadTooLargeException.class,
        org.springframework.web.multipart.MaxUploadSizeExceededException.class,
    })
    public ResponseEntity<ErrorResponse> handlePayloadTooLarge(
        Exception ex,
        HttpServletRequest request
    ) {
        log.warn(
            "Payload too large on {}: {}",
            request.getRequestURI(),
            ex.getMessage()
        );
        ErrorResponse error = buildErrorResponse(
            HttpStatus.CONTENT_TOO_LARGE,
            ex.getMessage(),
            request
        );
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).body(error);
    }

    @ExceptionHandler(
        com.platform.app.document.domain.exception.StorageException.class
    )
    public ResponseEntity<ErrorResponse> handleStorageException(
        com.platform.app.document.domain.exception.StorageException ex,
        HttpServletRequest request
    ) {
        log.error(
            "Storage error on {}: {}",
            request.getRequestURI(),
            ex.getMessage(),
            ex
        );
        ErrorResponse error = buildErrorResponse(
            HttpStatus.BAD_GATEWAY,
            "Object storage error: " + ex.getMessage(),
            request
        );
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error);
    }

    @ExceptionHandler({
        EmptyRolesException.class,
        SelfRoleRevocationException.class,
        InvalidDepartmentCodeException.class,
        com.platform.app.document.domain.exception.DocumentValidationException.class,
        IllegalArgumentException.class,
    })
    public ResponseEntity<ErrorResponse> handleBadRequestExceptions(
        RuntimeException ex,
        HttpServletRequest request
    ) {
        log.warn(
            "Bad request rejection on {}: {}",
            request.getRequestURI(),
            ex.getMessage()
        );
        ErrorResponse error = buildErrorResponse(
            HttpStatus.BAD_REQUEST,
            ex.getMessage(),
            request
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(
        com.platform.app.document.domain.exception.DocumentNotFoundException.class
    )
    public ResponseEntity<ErrorResponse> handleDocumentNotFound(
        com.platform.app.document.domain.exception.DocumentNotFoundException ex,
        HttpServletRequest request
    ) {
        log.warn(
            "Document not found on {}: {}",
            request.getRequestURI(),
            ex.getMessage()
        );
        ErrorResponse error = buildErrorResponse(
            HttpStatus.NOT_FOUND,
            ex.getMessage(),
            request
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(
        org.springframework.web.servlet.resource.NoResourceFoundException.class
    )
    public ResponseEntity<ErrorResponse> handleNoResourceFound(
        org.springframework.web.servlet.resource.NoResourceFoundException ex,
        HttpServletRequest request
    ) {
        log.warn(
            "Resource not found on {}: {}",
            request.getRequestURI(),
            ex.getMessage()
        );
        ErrorResponse error = buildErrorResponse(
            HttpStatus.NOT_FOUND,
            ex.getMessage(),
            request
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler({
        org.springframework.security.access.AccessDeniedException.class,
        com.platform.app.document.domain.exception.DocumentAccessDeniedException.class,
    })
    public ResponseEntity<ErrorResponse> handleAccessDenied(
        RuntimeException ex,
        HttpServletRequest request
    ) {
        log.warn(
            "Access denied on {}: {}",
            request.getRequestURI(),
            ex.getMessage()
        );
        ErrorResponse error = buildErrorResponse(
            HttpStatus.FORBIDDEN,
            ex.getMessage(),
            request
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
        Exception ex,
        HttpServletRequest request
    ) {
        log.error(
            "Unhandled exception at {}: {}",
            request.getRequestURI(),
            ex.getMessage(),
            ex
        );
        ErrorResponse error = buildErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected internal error occurred",
            request
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            error
        );
    }

    private ErrorResponse buildErrorResponse(
        HttpStatus status,
        String message,
        HttpServletRequest request
    ) {
        return ErrorResponse.builder()
            .status(status.value())
            .error(status.getReasonPhrase())
            .message(message)
            .timestamp(Instant.now())
            .path(request.getRequestURI())
            .build();
    }
}
