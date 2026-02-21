package com.example.tokencache.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(TokenAcquisitionException.class)
  public ResponseEntity<ErrorResponse> handleTokenAcquisition(TokenAcquisitionException ex) {
    return ResponseEntity.status(502)
        .body(new ErrorResponse("TOKEN_ACQUISITION_FAILED", ex.getMessage()));
  }

  public record ErrorResponse(String error, String message) {
  }
}
