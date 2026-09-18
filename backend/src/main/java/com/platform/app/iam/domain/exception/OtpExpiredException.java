package com.platform.app.iam.domain.exception;

public class OtpExpiredException extends RuntimeException {
  public OtpExpiredException(String message) {
    super(message);
  }
}
