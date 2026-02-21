package com.example.tokencache.controller;

import com.example.tokencache.dto.EnmLoginRequest;
import com.example.tokencache.service.EnmService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/enm")
public class EnmController {

  private final EnmService service;

  public EnmController(EnmService service) {
    this.service = service;
  }

  @PostMapping("/login")
  public ResponseEntity<String> login(@Valid @RequestBody EnmLoginRequest request) {
    return service.login(request);
  }
}
