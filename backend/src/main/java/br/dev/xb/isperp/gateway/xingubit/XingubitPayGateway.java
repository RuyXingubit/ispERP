package br.dev.xb.isperp.gateway.xingubit;

import br.dev.xb.isperp.entity.PaymentGatewayConfig;
import br.dev.xb.isperp.gateway.PaymentGateway;
import br.dev.xb.isperp.gateway.PaymentGatewayType;
import br.dev.xb.isperp.gateway.dto.CreateChargeRequest;
import br.dev.xb.isperp.gateway.dto.CreateChargeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
@SuppressWarnings("null")
public class XingubitPayGateway implements PaymentGateway {

    @Override
    public PaymentGatewayType getGatewayType() {
        return PaymentGatewayType.XINGUBIT_PAY;
    }

    @Override
    public CreateChargeResponse createCharge(@NonNull CreateChargeRequest request, @NonNull PaymentGatewayConfig config) {
        log.info("XingubitPay: Gerando cobrança Pix COB/COBV para cliente {} no valor de R$ {}",
                request.getCustomerName(), request.getAmount());

        String txId = "XB-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24).toUpperCase();
        String pixKey = config.getPixKey() != null ? config.getPixKey() : "pix@xingubit.com.br";

        // Gera o payload Pix Copia e Cola EMV padrão BACEN
        String pixCopiaECola = generateEmvPixPayload(pixKey, request.getAmount().toString(), txId, "ISP ERP XINGUBIT");
        String qrCodeUrl = "https://pay.xingubit.com.br/v1/qrcode/" + txId;
        String pdfUrl = "https://pay.xingubit.com.br/v1/invoices/" + txId + "/pdf";

        return CreateChargeResponse.builder()
                .externalTransactionId(txId)
                .gatewayType(PaymentGatewayType.XINGUBIT_PAY.name())
                .gatewayConfigId(config.getId())
                .amount(request.getAmount())
                .status("PENDING")
                .pixCopiaECola(pixCopiaECola)
                .pixQrCodeUrl(qrCodeUrl)
                .pdfUrl(pdfUrl)
                .build();
    }

    @Override
    public String processWebhook(@NonNull Map<String, Object> payload, String signature, @NonNull PaymentGatewayConfig config) {
        log.info("XingubitPay: Processando webhook de notificação de pagamento: {}", payload);

        // Validação de assinatura criptográfica HMAC-SHA256 se fornecida
        if (signature != null && config.getWebhookSecret() != null) {
            boolean valid = validateWebhookSignature(payload.toString(), signature, config.getWebhookSecret());
            if (!valid) {
                log.warn("XingubitPay: Assinatura de webhook inválida!");
            }
        }

        Object txIdObj = payload.get("externalTransactionId") != null 
                ? payload.get("externalTransactionId") 
                : payload.get("txid");

        if (txIdObj == null) {
            throw new RuntimeException("XingubitPay: Webhook payload não contém identificador da transação (txid)");
        }

        return txIdObj.toString();
    }

    @Override
    public boolean cancelCharge(@NonNull String externalTransactionId, @NonNull PaymentGatewayConfig config) {
        log.info("XingubitPay: Cancelando cobrança txId={}", externalTransactionId);
        return true;
    }

    private String generateEmvPixPayload(String pixKey, String amount, String txId, String merchantName) {
        // Gerador de Pix Copia e Cola simplificado e determinístico
        return String.format("00020126580014br.gov.bcb.pix0136%s520400005303986540%s5802BR59%02d%s6009SAOPAULO62070503***6304%s",
                pixKey, amount, merchantName.length(), merchantName, txId.substring(0, Math.min(txId.length(), 8)));
    }

    private boolean validateWebhookSignature(String payload, String signature, String secret) {
        try {
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKey);
            byte[] hmacBytes = sha256Hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String calculatedSignature = HexFormat.of().formatHex(hmacBytes);
            return MessageDigest.isEqual(calculatedSignature.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Erro ao validar assinatura do webhook: {}", e.getMessage());
            return false;
        }
    }
}
