package com.platform.app.shared.exception;

public class DomainRuleViolationException extends AppException {

    public DomainRuleViolationException(String message) {
        super(ErrorCode.DOMAIN_RULE_VIOLATION, message);
    }
}
