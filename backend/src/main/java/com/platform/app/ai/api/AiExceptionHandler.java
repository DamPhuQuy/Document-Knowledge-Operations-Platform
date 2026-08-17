package com.platform.app.ai.api;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.platform.app.ai.domain.exception.LlmProviderException;
import com.platform.app.ai.domain.exception.LlmSchemaValidationException;
import com.platform.app.ai.domain.exception.LlmTimeoutException;
import com.platform.app.shared.dto.ApiResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice(basePackages = "com.platform.app.ai")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AiExceptionHandler {

    @ExceptionHandler(LlmTimeoutException.class)
    public ResponseEntity<ApiResponse<Void>> handleLlmTimeoutException(LlmTimeoutException ex) {
        log.warn("AI service timeout: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.GATEWAY_TIMEOUT)
            .body(ApiResponse.error("AI service timed out: " + ex.getMessage()));
    }

    @ExceptionHandler(LlmProviderException.class)
    public ResponseEntity<ApiResponse<Void>> handleLlmProviderException(LlmProviderException ex) {
        log.warn("AI upstream provider error: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.BAD_GATEWAY)
            .body(ApiResponse.error("AI provider error: " + ex.getMessage()));
    }

    @ExceptionHandler(LlmSchemaValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleLlmSchemaValidationException(LlmSchemaValidationException ex) {
        log.warn("AI structured schema error: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(ApiResponse.error("Failed to parse AI structured response: " + ex.getMessage()));
    }
}
