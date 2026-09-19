package com.platform.app.iam.infrastructure.adapters.primary.rest;

import static org.assertj.core.api.Assertions.assertThat;

import com.platform.app.document.domain.exception.DocumentAccessDeniedException;
import com.platform.app.document.domain.exception.DocumentNotFoundException;
import com.platform.app.document.domain.exception.DocumentValidationException;
import com.platform.app.document.domain.exception.PayloadTooLargeException;
import com.platform.app.document.domain.exception.StorageException;
import com.platform.app.document.domain.exception.UnsupportedMediaTypeException;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;

class RestExceptionHandlerTest {

    private RestExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new RestExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/test");
    }

    @Test
    @DisplayName("Should map InvalidCredentialsException to 401 Unauthorized")
    void shouldMapInvalidCredentialsTo401() {
        ResponseEntity<ErrorResponse> res = handler.handleInvalidCredentials(
            new InvalidCredentialsException("Invalid password"),
            request
        );
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(res.getBody().status()).isEqualTo(401);
        assertThat(res.getBody().message()).isEqualTo("Invalid password");
        assertThat(res.getBody().path()).isEqualTo("/api/v1/test");
    }

    @Test
    @DisplayName("Should map AccountDisabledException to 403 Forbidden")
    void shouldMapAccountDisabledTo403() {
        ResponseEntity<ErrorResponse> res = handler.handleAccountDisabled(
            new AccountDisabledException("Account disabled"),
            request
        );
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(res.getBody().status()).isEqualTo(403);
    }

    @Test
    @DisplayName("Should map AccountLockedException to 423 Locked")
    void shouldMapAccountLockedTo423() {
        ResponseEntity<ErrorResponse> res = handler.handleAccountLocked(
            new AccountLockedException("Account locked"),
            request
        );
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.LOCKED);
        assertThat(res.getBody().status()).isEqualTo(423);
    }

    @Test
    @DisplayName("Should map DepartmentNotFoundException to 404 Not Found")
    void shouldMapDepartmentNotFoundTo404() {
        ResponseEntity<ErrorResponse> res = handler.handleDepartmentNotFound(
            new DepartmentNotFoundException("Dept not found"),
            request
        );
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(res.getBody().status()).isEqualTo(404);
    }

    @Test
    @DisplayName("Should map DepartmentCodeConflictException to 409 Conflict")
    void shouldMapDepartmentCodeConflictTo409() {
        ResponseEntity<ErrorResponse> res = handler.handleDepartmentCodeConflict(
            new DepartmentCodeConflictException("Code exists"),
            request
        );
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(res.getBody().status()).isEqualTo(409);
    }

    @Test
    @DisplayName("Should map EmailAlreadyExistsException to 409 Conflict")
    void shouldMapEmailAlreadyExistsTo409() {
        ResponseEntity<ErrorResponse> res = handler.handleEmailAlreadyExists(
            new EmailAlreadyExistsException("Email exists"),
            request
        );
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(res.getBody().status()).isEqualTo(409);
    }

    @Test
    @DisplayName("Should map InvalidOtpException and OtpExpiredException to 400 Bad Request")
    void shouldMapOtpExceptionsTo400() {
        ResponseEntity<ErrorResponse> res1 = handler.handleOtpException(
            new InvalidOtpException("Bad OTP"),
            request
        );
        assertThat(res1.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ResponseEntity<ErrorResponse> res2 = handler.handleOtpException(
            new OtpExpiredException("Expired OTP"),
            request
        );
        assertThat(res2.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Should map UnsupportedMediaTypeException to 415 Unsupported Media Type")
    void shouldMapUnsupportedMediaTypeTo415() {
        ResponseEntity<ErrorResponse> res = handler.handleUnsupportedMediaType(
            new UnsupportedMediaTypeException("Invalid type"),
            request
        );
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(res.getBody().status()).isEqualTo(415);
    }

    @Test
    @DisplayName("Should map PayloadTooLargeException to 413 Content Too Large")
    void shouldMapPayloadTooLargeTo413() {
        ResponseEntity<ErrorResponse> res = handler.handlePayloadTooLarge(
            new PayloadTooLargeException("Exceeded 50MB"),
            request
        );
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONTENT_TOO_LARGE);
        assertThat(res.getBody().status()).isEqualTo(413);
    }

    @Test
    @DisplayName("Should map StorageException to 502 Bad Gateway")
    void shouldMapStorageExceptionTo502() {
        ResponseEntity<ErrorResponse> res = handler.handleStorageException(
            new StorageException("S3 network error"),
            request
        );
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(res.getBody().status()).isEqualTo(502);
    }

    @Test
    @DisplayName("Should map DocumentNotFoundException to 404 Not Found")
    void shouldMapDocumentNotFoundTo404() {
        ResponseEntity<ErrorResponse> res = handler.handleDocumentNotFound(
            new DocumentNotFoundException("Doc not found"),
            request
        );
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Should map AccessDeniedException and DocumentAccessDeniedException to 403 Forbidden")
    void shouldMapAccessDeniedTo403() {
        ResponseEntity<ErrorResponse> res1 = handler.handleAccessDenied(
            new AccessDeniedException("Forbidden"),
            request
        );
        assertThat(res1.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<ErrorResponse> res2 = handler.handleAccessDenied(
            new DocumentAccessDeniedException("Doc denied"),
            request
        );
        assertThat(res2.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Should map domain bad request exceptions to 400 Bad Request")
    void shouldMapDomainBadRequestExceptionsTo400() {
        assertThat(handler.handleBadRequestExceptions(new EmptyRolesException("Empty"), request).getStatusCode())
            .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(handler.handleBadRequestExceptions(new SelfRoleRevocationException("Self"), request).getStatusCode())
            .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(handler.handleBadRequestExceptions(new InvalidDepartmentCodeException("Code"), request).getStatusCode())
            .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(handler.handleBadRequestExceptions(new DocumentValidationException("Invalid"), request).getStatusCode())
            .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Should map generic Exception to 500 Internal Server Error")
    void shouldMapGenericExceptionTo500() {
        ResponseEntity<ErrorResponse> res = handler.handleGenericException(
            new RuntimeException("Crash"),
            request
        );
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(res.getBody().status()).isEqualTo(500);
        assertThat(res.getBody().message()).isEqualTo("An unexpected internal error occurred");
    }
}
