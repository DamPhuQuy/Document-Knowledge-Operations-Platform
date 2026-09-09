package com.platform.app.iam.domain.exception;

public class AccountLockedException extends RuntimeException {
  public AccountLockedException(String message) {
    super(message);
  }
}
