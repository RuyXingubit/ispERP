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

@SuppressWarnings("null")
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

    @Test
    @DisplayName("Deve aceitar webhook com assinatura HMAC-SHA256 válida")
    void shouldValidateAndAcceptLegitimateHmacSignature() {
        Map<String, Object> payload = Map.of(
                "txid", "XB-TEST87654321",
                "paidAmount", "149.90",
                "status", "PAID"
        );

        String signature = gateway.calculateSignature(payload.toString(), sampleConfig.getWebhookSecret());
        String txId = gateway.processWebhook(payload, signature, sampleConfig);

        assertEquals("XB-TEST87654321", txId);
    }

    @Test
    @DisplayName("Deve rejeitar webhook com assinatura HMAC adulterada com SecurityException")
    void shouldRejectTamperedHmacSignatureWithSecurityException() {
        Map<String, Object> payload = Map.of(
                "txid", "XB-FORGED-001",
                "paidAmount", "10.00"
        );

        assertThrows(SecurityException.class, () -> {
            gateway.processWebhook(payload, "invalid_or_forged_signature_12345", sampleConfig);
        });
    }

    @Test
    @DisplayName("Deve simular emissão de NFCom modelo 62 com chave de 44 dígitos")
    void shouldIssueNfcomWithCorrectFormat() {
        var response = gateway.issueNfcom(
                "XB-TEST-123",
                "12345678909",
                "Cliente Teste",
                new BigDecimal("99.90"),
                sampleConfig
        );

        assertNotNull(response);
        assertEquals(44, response.getNfcomKey().length());
        assertTrue(response.getNfcomKey().contains("62")); // Modelo 62
        assertEquals("ISSUED", response.getStatus());
        assertNotNull(response.getXmlUrl());
        assertNotNull(response.getPdfUrl());
    }
}
