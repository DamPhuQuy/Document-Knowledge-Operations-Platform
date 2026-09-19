package com.platform.app.iam.domain.exception;

public class SelfRoleRevocationException extends RuntimeException {

    public SelfRoleRevocationException(String message) {
        super(message);
    }
}
