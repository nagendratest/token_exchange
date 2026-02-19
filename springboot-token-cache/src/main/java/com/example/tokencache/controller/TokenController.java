package com.example.tokencache.controller;

import com.example.tokencache.dto.TokenRequest;
import com.example.tokencache.dto.TokenResult;
import com.example.tokencache.service.OAuthTokenService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TokenController {

  private final OAuthTokenService tokenService;

  public TokenController(OAuthTokenService tokenService) {
    this.tokenService = tokenService;
  }

  @PostMapping("/token")
  public TokenResult token(@Valid @RequestBody TokenRequest request) {
    return tokenService.getToken(request);
  }
}
