package com.example.tokencache.controller;

import com.example.tokencache.dto.EOrchestratorTokenRequest;
import com.example.tokencache.service.EOrchestratorService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EOrchestratorController.class)
class EOrchestratorControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private EOrchestratorService service;

  @Test
  void tokenReturnsUpstreamResponse() throws Exception {
    Mockito.when(service.fetchToken(Mockito.any()))
        .thenReturn(ResponseEntity.ok("token-response"));

    String payload = """
        {"username":"user1","password":"pass1","tenantId":"TENANT"}
        """;

    mockMvc.perform(post("/eorchestrator/tokens")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
        .andExpect(status().isOk())
        .andExpect(content().string("token-response"));

    ArgumentCaptor<EOrchestratorTokenRequest> captor =
        ArgumentCaptor.forClass(EOrchestratorTokenRequest.class);
    Mockito.verify(service).fetchToken(captor.capture());
    EOrchestratorTokenRequest request = captor.getValue();
    assertThat(request.username()).isEqualTo("user1");
    assertThat(request.password()).isEqualTo("pass1");
    assertThat(request.tenantId()).isEqualTo("TENANT");
  }

  @Test
  void missingTenantReturns400() throws Exception {
    String payload = """
        {"username":"user1","password":"pass1"}
        """;

    mockMvc.perform(post("/eorchestrator/tokens")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
        .andExpect(status().isBadRequest());

    Mockito.verifyNoInteractions(service);
  }
}
