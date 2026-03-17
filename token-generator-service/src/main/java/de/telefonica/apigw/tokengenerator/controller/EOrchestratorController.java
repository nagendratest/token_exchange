package de.telefonica.apigw.tokengenerator.controller;

import de.telefonica.apigw.tokengenerator.dto.EOrchestratorTokenRequest;
import de.telefonica.apigw.tokengenerator.dto.EOrchestratorTokenResponse;
import de.telefonica.apigw.tokengenerator.service.EOrchestratorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/eorchestrator")
public class EOrchestratorController {

  private final EOrchestratorService service;

  public EOrchestratorController(EOrchestratorService service) {
    this.service = service;
  }

  @PostMapping("/tokens")
  public ResponseEntity<EOrchestratorTokenResponse> token(
      @Valid @RequestBody EOrchestratorTokenRequest request
  ) {
    return service.fetchToken(request);
  }
}
