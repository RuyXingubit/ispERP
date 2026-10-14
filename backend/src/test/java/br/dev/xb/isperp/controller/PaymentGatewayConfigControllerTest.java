package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.entity.PaymentGatewayConfig;
import br.dev.xb.isperp.gateway.PaymentGatewayType;
import br.dev.xb.isperp.repository.PaymentGatewayConfigRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PaymentGatewayConfigController.class)
@AutoConfigureMockMvc(addFilters = false)
@SuppressWarnings("null")
class PaymentGatewayConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentGatewayConfigRepository configRepository;

    @Test
    @DisplayName("GET /payment-gateways deve retornar lista de gateways")
    void testGetAllConfigs() throws Exception {
        PaymentGatewayConfig config = PaymentGatewayConfig.builder()
                .id(UUID.randomUUID())
                .name("Asaas Gateway")
                .gatewayType(PaymentGatewayType.ASAAS)
                .active(true)
                .sandbox(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(configRepository.findAll()).thenReturn(List.of(config));

        mockMvc.perform(get("/payment-gateways"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Asaas Gateway"))
                .andExpect(jsonPath("$[0].gatewayType").value("ASAAS"));
    }

    @Test
    @DisplayName("GET /payment-gateways/{id} deve retornar gateway por id")
    void testGetConfigById() throws Exception {
        UUID id = UUID.randomUUID();
        PaymentGatewayConfig config = PaymentGatewayConfig.builder()
                .id(id)
                .name("Asaas Gateway")
                .gatewayType(PaymentGatewayType.ASAAS)
                .active(true)
                .sandbox(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(configRepository.findById(id)).thenReturn(Optional.of(config));

        mockMvc.perform(get("/payment-gateways/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Asaas Gateway"));
    }

    @Test
    @DisplayName("Segurança: GET /payment-gateways deve mascarar secretKey e webhookSecret e POST deve preservar segredo original quando enviado mascarado")
    void shouldMaskSecretsInGetAndPreserveWhenSavingMaskedValue() throws Exception {
        UUID id = br.dev.xb.isperp.util.UuidCreatorUtils.generateUuidV7();
        PaymentGatewayConfig config = PaymentGatewayConfig.builder()
                .id(id)
                .name("Xingubit Pay Prod")
                .gatewayType(PaymentGatewayType.XINGUBIT_PAY)
                .apiKey("pk_live_123456")
                .secretKey("sk_live_supersecret9876")
                .webhookSecret("whsec_topsecret5432")
                .active(true)
                .sandbox(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(configRepository.findById(id)).thenReturn(Optional.of(config));
        when(configRepository.save(any(PaymentGatewayConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        // 1. Verifica que o GET retorna os segredos mascarados com os 4 últimos caracteres
        mockMvc.perform(get("/payment-gateways/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.secretKey").value("••••••••9876"))
                .andExpect(jsonPath("$.webhookSecret").value("••••••••5432"));

        // 2. Verifica que ao salvar enviando a máscara, o segredo original no banco é preservado
        String jsonPayload = String.format(
                "{\"id\":\"%s\",\"name\":\"Xingubit Pay Prod\",\"gatewayType\":\"XINGUBIT_PAY\",\"apiKey\":\"pk_live_123456\",\"secretKey\":\"••••••••9876\",\"webhookSecret\":\"••••••••5432\"}",
                id
        );

        mockMvc.perform(post("/payment-gateways")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.secretKey").value("••••••••9876"))
                .andExpect(jsonPath("$.webhookSecret").value("••••••••5432"));
    }
}

