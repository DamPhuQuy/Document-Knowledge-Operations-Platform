package com.platform.app.shared.exception;

public class UnauthorizedException extends AppException {

    public UnauthorizedException(String message) {
        super(ErrorCode.UNAUTHORIZED, message);
    }
}
