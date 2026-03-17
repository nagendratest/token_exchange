package de.telefonica.apigw.tokengenerator.controller;

import de.telefonica.apigw.tokengenerator.dto.EnmLoginRequest;
import de.telefonica.apigw.tokengenerator.dto.EnmLoginResponse;
import de.telefonica.apigw.tokengenerator.service.EnmService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/enm")
public class EnmController {

  private static final String TESTUSER_MARKER = "testuser";
  private static final String TESTUSER_FAKE_TOKEN = "sdfsafdsdfsdwe123344sdsadfcefwe3easdfsadf";

  private final EnmService service;

  public EnmController(EnmService service) {
    this.service = service;
  }

  @PostMapping("/login")
  public ResponseEntity<EnmLoginResponse> login(@Valid @RequestBody EnmLoginRequest request) {
    String username = request.username();
    if (username != null && username.toLowerCase().contains(TESTUSER_MARKER)) {
      return ResponseEntity.ok()
          .contentType(MediaType.APPLICATION_JSON)
          .body(new EnmLoginResponse(TESTUSER_FAKE_TOKEN));
    }
    return service.login(request);
  }
}
