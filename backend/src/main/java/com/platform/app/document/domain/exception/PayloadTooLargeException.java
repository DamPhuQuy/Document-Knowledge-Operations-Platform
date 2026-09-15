package com.platform.app.document.domain.exception;

public class PayloadTooLargeException extends RuntimeException {
  public PayloadTooLargeException(String message) {
    super(message);
  }
}
