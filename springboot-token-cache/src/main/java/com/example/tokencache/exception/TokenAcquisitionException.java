package com.example.tokencache.exception;

public class TokenAcquisitionException extends RuntimeException {

  public TokenAcquisitionException(String message) {
    super(message);
  }

  public TokenAcquisitionException(String message, Throwable cause) {
    super(message, cause);
  }
}
