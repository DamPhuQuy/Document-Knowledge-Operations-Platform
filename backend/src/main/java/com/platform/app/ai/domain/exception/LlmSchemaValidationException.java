package com.platform.app.ai.domain.exception;

public class LlmSchemaValidationException extends RuntimeException {
    public LlmSchemaValidationException(String message) {
        super(message);
    }

    public LlmSchemaValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
