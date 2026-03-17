package de.telefonica.apigw.tokengenerator.exception;

public class TokenAcquisitionException extends RuntimeException {

  private final Integer statusCode;

  public TokenAcquisitionException(String message) {
    super(message);
    this.statusCode = null;
  }

  public TokenAcquisitionException(String message, Throwable cause) {
    super(message, cause);
    this.statusCode = null;
  }

  public TokenAcquisitionException(String message, int statusCode) {
    super(message);
    this.statusCode = statusCode;
  }

  public TokenAcquisitionException(String message, int statusCode, Throwable cause) {
    super(message, cause);
    this.statusCode = statusCode;
  }

  public Integer getStatusCode() {
    return statusCode;
  }
}
