package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.dto.LoginRequest;
import br.dev.xb.isperp.api.dto.LoginResponse;
import br.dev.xb.isperp.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@SuppressWarnings("null")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("POST /auth/login - Deve autenticar com credenciais válidas")
    void shouldLoginSuccessfully() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin@nexusfibra.com.br");
        request.setPassword("password123");

        LoginResponse response = new LoginResponse()
                .success(true)
                .message("Login realizado com sucesso")
                .token("mock-jwt-token-123")
                .username("admin@nexusfibra.com.br")
                .role("ADMIN");

        when(authService.authenticate(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.token").value("mock-jwt-token-123"))
                .andExpect(jsonPath("$.username").value("admin@nexusfibra.com.br"))
                .andExpect(jsonPath("$.role").value("ADMIN"));

        verify(authService).authenticate(any(LoginRequest.class));
    }

    @Test
    @DisplayName("POST /auth/login - Deve retornar 400 em caso de credenciais inválidas")
    void shouldReturnBadRequestOnInvalidCredentials() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin@nexusfibra.com.br");
        request.setPassword("wrong-password");

        when(authService.authenticate(any(LoginRequest.class)))
                .thenThrow(new RuntimeException("Senha inválida"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Credenciais inválidas"));
    }
}
