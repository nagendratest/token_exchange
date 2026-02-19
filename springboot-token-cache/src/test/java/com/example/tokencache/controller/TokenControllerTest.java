package com.example.tokencache.controller;

import com.example.tokencache.client.KeycloakTokenClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "keycloak.tenants.ECM.token-url=http://keycloak:8080/realms/demo/protocol/openid-connect/token",
        "token.cache.skew-seconds=30",
        "token.cache.max-size=1000"
    }
)
@AutoConfigureMockMvc
class TokenControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private KeycloakTokenClient keycloakTokenClient;

  @Test
  void unknownTenantReturns400() throws Exception {
    String payload = "{\"tenantId\":\"UNKNOWN\",\"clientId\":\"client\",\"clientSecret\":\"secret\"}";
    mockMvc.perform(post("/token")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
        .andExpect(status().isBadRequest());

    Mockito.verifyNoInteractions(keycloakTokenClient);
  }
}
