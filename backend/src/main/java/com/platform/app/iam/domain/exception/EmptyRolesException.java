package com.platform.app.iam.domain.exception;

public class EmptyRolesException extends RuntimeException {

    public EmptyRolesException(String message) {
        super(message);
    }
}
