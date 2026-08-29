package br.dev.xb.isperp.gateway.xingubit;

import br.dev.xb.isperp.entity.PaymentGatewayConfig;
import br.dev.xb.isperp.gateway.PaymentGatewayType;
import br.dev.xb.isperp.gateway.dto.CreateChargeRequest;
import br.dev.xb.isperp.gateway.dto.CreateChargeResponse;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class XingubitPayGatewayTest {

    private XingubitPayGateway gateway;
    private PaymentGatewayConfig sampleConfig;

    @BeforeEach
    void setUp() {
        gateway = new XingubitPayGateway();
        sampleConfig = PaymentGatewayConfig.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .gatewayType(PaymentGatewayType.XINGUBIT_PAY)
                .name("Xingubit Pay Teste")
                .apiKey("test_key")
                .secretKey("test_secret")
                .webhookSecret("secret_wh_123")
                .pixKey("pix@xingubit.com.br")
                .build();
    }

    @Test
    @DisplayName("Deve gerar cobrança Pix com Copia e Cola e QR Code")
    void shouldGeneratePixCharge() {
        CreateChargeRequest request = CreateChargeRequest.builder()
                .invoiceId(UuidCreatorUtils.generateUuidV7())
                .customerName("Ruy Barbosa")
                .customerCpf("12345678909")
                .amount(new BigDecimal("119.90"))
                .dueDate(LocalDate.now().plusDays(10))
                .build();

        CreateChargeResponse response = gateway.createCharge(request, sampleConfig);

        assertNotNull(response);
        assertTrue(response.getExternalTransactionId().startsWith("XB-"));
        assertNotNull(response.getPixCopiaECola());
        assertNotNull(response.getPixQrCodeUrl());
        assertEquals("PENDING", response.getStatus());
        assertEquals(PaymentGatewayType.XINGUBIT_PAY.name(), response.getGatewayType());
    }

    @Test
    @DisplayName("Deve extrair txId ao processar webhook do Xingubit Pay")
    void shouldExtractTxIdFromWebhookPayload() {
        Map<String, Object> payload = Map.of(
                "txid", "XB-TEST12345678",
                "paidAmount", "119.90",
                "status", "PAID"
        );

        String txId = gateway.processWebhook(payload, null, sampleConfig);

        assertEquals("XB-TEST12345678", txId);
    }
}
