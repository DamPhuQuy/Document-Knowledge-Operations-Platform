package com.platform.app.document.domain.exception;

public class DocumentValidationException extends RuntimeException {
  public DocumentValidationException(String message) {
    super(message);
  }
}
