package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.service.PaymentWebhookService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PaymentWebhookController.class)
@AutoConfigureMockMvc(addFilters = false)
@SuppressWarnings("null")
class PaymentWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentWebhookService paymentWebhookService;

    @Test
    @DisplayName("POST /webhooks/payments/{gatewayType} deve processar webhook com sucesso")
    void testHandlePaymentWebhook() throws Exception {
        doNothing().when(paymentWebhookService).processPaymentWebhook(eq("ASAAS"), any(), any());

        String json = """
                {
                    "event": "PAYMENT_RECEIVED",
                    "payment": {
                        "id": "pay_123456",
                        "value": 150.00
                    }
                }
                """;

        mockMvc.perform(post("/webhooks/payments/ASAAS")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.received").value(true))
                .andExpect(jsonPath("$.status").value("PROCESSED"));
    }
}
