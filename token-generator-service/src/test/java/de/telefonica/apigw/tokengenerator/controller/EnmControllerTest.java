package de.telefonica.apigw.tokengenerator.controller;

import de.telefonica.apigw.tokengenerator.dto.EnmLoginRequest;
import de.telefonica.apigw.tokengenerator.dto.EnmLoginResponse;
import de.telefonica.apigw.tokengenerator.service.EnmService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EnmController.class)
class EnmControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private EnmService service;

  @Test
  void loginReturnsUpstreamResponse() throws Exception {
    Mockito.when(service.login(Mockito.any()))
        .thenReturn(ResponseEntity.ok(new EnmLoginResponse("login-ok")));

    String payload = """
        {"username":"admin","password":"secret","tenantId":"PacketCore5G"}
        """;

    mockMvc.perform(post("/enm/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
        .andExpect(status().isOk())
        .andExpect(content().json("{\"access_token\":\"login-ok\"}"));

    ArgumentCaptor<EnmLoginRequest> captor = ArgumentCaptor.forClass(EnmLoginRequest.class);
    Mockito.verify(service).login(captor.capture());
    EnmLoginRequest request = captor.getValue();
    assertThat(request.username()).isEqualTo("admin");
    assertThat(request.password()).isEqualTo("secret");
    assertThat(request.tenantId()).isEqualTo("PacketCore5G");
  }

  @Test
  void missingPasswordReturns400() throws Exception {
    String payload = """
        {"username":"admin","tenantId":"PacketCore5G"}
        """;

    mockMvc.perform(post("/enm/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
        .andExpect(status().isBadRequest());

    Mockito.verifyNoInteractions(service);
  }

  @Test
  void missingTenantIdReturns400() throws Exception {
    String payload = """
        {"username":"admin","password":"secret"}
        """;

    mockMvc.perform(post("/enm/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
        .andExpect(status().isBadRequest());

    Mockito.verifyNoInteractions(service);
  }
}
