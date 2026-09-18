package com.platform.app.document.domain.exception;

public class DocumentAccessDeniedException extends RuntimeException {

  public DocumentAccessDeniedException(String message) {
    super(message);
  }
}
