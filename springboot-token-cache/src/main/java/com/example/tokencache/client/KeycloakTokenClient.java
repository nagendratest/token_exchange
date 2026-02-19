package com.example.tokencache.client;

import com.example.tokencache.exception.TokenAcquisitionException;
import com.example.tokencache.model.TokenResponse;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class KeycloakTokenClient {

  private final RestClient restClient;

  public KeycloakTokenClient(RestClient.Builder builder) {
    this.restClient = builder.build();
  }

  public TokenResponse fetchToken(String tokenUrl, String scope, String clientId, String clientSecret) {
    int maxRetries = 3;
    long[] backoffMillis = new long[]{200L, 500L, 1000L};

    for (int attempt = 0; attempt <= maxRetries; attempt++) {
      if (attempt > 0) {
        sleep(backoffMillis[Math.min(attempt - 1, backoffMillis.length - 1)]);
      }
      try {
        TokenResponse response = doRequest(tokenUrl, scope, clientId, clientSecret);
        if (response == null) {
          throw new TokenAcquisitionException("Empty response from Keycloak");
        }
        return response;
      } catch (RetryableTokenException | ResourceAccessException ex) {
        if (attempt == maxRetries) {
          throw new TokenAcquisitionException("Failed to acquire token from Keycloak", ex);
        }
      } catch (RestClientResponseException ex) {
        if (ex.getStatusCode().is5xxServerError()) {
          if (attempt == maxRetries) {
            throw new TokenAcquisitionException("Failed to acquire token from Keycloak", ex);
          }
        } else {
          throw new TokenAcquisitionException("Keycloak 4xx: " + ex.getStatusCode().value(), ex);
        }
      }
    }

    throw new TokenAcquisitionException("Failed to acquire token from Keycloak");
  }

  private TokenResponse doRequest(String tokenUrl, String scope, String clientId, String clientSecret) {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", "client_credentials");
    form.add("client_id", clientId);
    form.add("client_secret", clientSecret);
    if (scope != null && !scope.isBlank()) {
      form.add("scope", scope);
    }

    return restClient.post()
        .uri(tokenUrl)
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .body(form)
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
          throw new TokenAcquisitionException("Keycloak 4xx: " + response.getStatusCode().value());
        })
        .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
          throw new RetryableTokenException("Keycloak 5xx: " + response.getStatusCode().value());
        })
        .body(TokenResponse.class);
  }

  private void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }

  private static class RetryableTokenException extends RuntimeException {
    RetryableTokenException(String message) {
      super(message);
    }
  }
}
