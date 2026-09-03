package br.dev.xb.isperp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import br.dev.xb.isperp.dto.*;
import br.dev.xb.isperp.entity.Customer;
import br.dev.xb.isperp.service.ClientPortalService;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ClientPortalController.class)
@AutoConfigureMockMvc(addFilters = false)
@SuppressWarnings("null")
class ClientPortalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ClientPortalService clientPortalService;

    @Test
    @DisplayName("POST /portal/client/auth - Deve autenticar com CPF e retornar AUTHENTICATED")
    void shouldAuthenticateWithCpf() throws Exception {
        UUID customerId = UuidCreatorUtils.generateUuidV7();
        ClientAuthResponse authResponse = ClientAuthResponse.builder()
                .status("AUTHENTICATED")
                .message("Acesso liberado com sucesso.")
                .customerId(customerId)
                .customerName("João das Neves")
                .hasPin(false)
                .build();

        when(clientPortalService.authenticateClient(any(ClientAuthRequest.class))).thenReturn(authResponse);

        ClientAuthRequest request = ClientAuthRequest.builder()
                .document("52998224725")
                .build();

        mockMvc.perform(post("/portal/client/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AUTHENTICATED"))
                .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.customerName").value("João das Neves"));
    }

    @Test
    @DisplayName("POST /portal/client/auth - Deve exigir PIN quando cliente possui PIN cadastrado")
    void shouldRequirePinWhenConfigured() throws Exception {
        UUID customerId = UuidCreatorUtils.generateUuidV7();
        ClientAuthResponse authResponse = ClientAuthResponse.builder()
                .status("PIN_REQUIRED")
                .message("Informe seu PIN de 4 dígitos para prosseguir.")
                .customerId(customerId)
                .customerName("Maria Souza")
                .maskedDocument("123.***.***-45")
                .hasPin(true)
                .build();

        when(clientPortalService.authenticateClient(any(ClientAuthRequest.class))).thenReturn(authResponse);

        ClientAuthRequest request = ClientAuthRequest.builder()
                .document("12345678900")
                .build();

        mockMvc.perform(post("/portal/client/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PIN_REQUIRED"))
                .andExpect(jsonPath("$.hasPin").value(true));
    }

    @Test
    @DisplayName("POST /portal/client/pin - Deve cadastrar PIN de 4 dígitos")
    void shouldSetPinSuccessfully() throws Exception {
        UUID customerId = UuidCreatorUtils.generateUuidV7();
        SetClientPinRequest request = SetClientPinRequest.builder()
                .customerId(customerId)
                .newPin("1234")
                .build();

        mockMvc.perform(post("/portal/client/pin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("GET /portal/client/dashboard - Deve rejeitar com 401 quando nenhum identificador for fornecido")
    void shouldRejectAnonymousDashboardRequest() throws Exception {
        mockMvc.perform(get("/portal/client/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /portal/client/dashboard - Deve aceitar requisição com customerId fornecido")
    void shouldAcceptDashboardRequestWithCustomerId() throws Exception {
        UUID customerId = UuidCreatorUtils.generateUuidV7();
        Customer customer = Customer.builder().id(customerId).name("Cliente Teste").cpf("12345678900").build();
        ClientPortalDashboardDTO dto = ClientPortalDashboardDTO.builder().customer(customer).build();

        when(clientPortalService.getClientDashboard(customerId)).thenReturn(dto);

        mockMvc.perform(get("/portal/client/dashboard")
                        .param("customerId", customerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customer.name").value("Cliente Teste"));
    }
}
