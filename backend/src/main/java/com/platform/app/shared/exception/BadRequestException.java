package com.platform.app.shared.exception;

public class BadRequestException extends AppException {

    public BadRequestException(String message) {
        super(ErrorCode.BAD_REQUEST, message);
    }
}
