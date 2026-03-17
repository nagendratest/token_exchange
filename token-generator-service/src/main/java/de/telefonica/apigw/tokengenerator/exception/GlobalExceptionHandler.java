package de.telefonica.apigw.tokengenerator.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(TokenAcquisitionException.class)
  public ResponseEntity<ErrorResponse> handleTokenAcquisition(TokenAcquisitionException ex) {
    int status = ex.getStatusCode() != null ? ex.getStatusCode() : 502;
    return ResponseEntity.status(status)
        .body(new ErrorResponse("TOKEN_ACQUISITION_FAILED", ex.getMessage()));
  }

  public record ErrorResponse(String error, String message) {
  }
}
