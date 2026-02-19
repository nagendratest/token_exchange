package com.example.tokencache.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(UnknownTenantException.class)
  public ResponseEntity<ErrorResponse> handleUnknownTenant(UnknownTenantException ex) {
    return ResponseEntity.badRequest().body(new ErrorResponse("UNKNOWN_TENANT", ex.getMessage()));
  }

  @ExceptionHandler(TokenAcquisitionException.class)
  public ResponseEntity<ErrorResponse> handleTokenAcquisition(TokenAcquisitionException ex) {
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
        .body(new ErrorResponse("TOKEN_ACQUISITION_FAILED", ex.getMessage()));
  }

  public record ErrorResponse(String error, String message) {
  }
}
