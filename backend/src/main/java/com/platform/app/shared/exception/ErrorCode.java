package com.platform.app.shared.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "ERR_500", "An unexpected internal server error occurred"),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "ERR_400", "Bad request"),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "ERR_400_VALIDATION", "Request validation failed"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "ERR_401", "Unauthorized or invalid credentials"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "ERR_403", "Access denied. Insufficient permissions"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "ERR_404", "Resource not found"),
    RESOURCE_ALREADY_EXISTS(HttpStatus.CONFLICT, "ERR_409", "Resource already exists"),
    DOMAIN_RULE_VIOLATION(HttpStatus.UNPROCESSABLE_CONTENT, "ERR_422_DOMAIN", "Business domain rule violation"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "ERR_USER_404", "User not found"),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "ERR_EMAIL_409", "Email is already registered");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

}
