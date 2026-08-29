package br.dev.xb.isperp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import br.dev.xb.isperp.entity.Invoice;
import br.dev.xb.isperp.entity.PaymentTransaction;
import br.dev.xb.isperp.gateway.PaymentGateway;
import br.dev.xb.isperp.gateway.PaymentGatewayResolver;
import br.dev.xb.isperp.gateway.PaymentGatewayType;
import br.dev.xb.isperp.repository.PaymentTransactionRepository;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class PaymentWebhookService {

    private final PaymentGatewayResolver gatewayResolver;
    private final InvoiceService invoiceService;
    private final PaymentTransactionRepository transactionRepository;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    @Transactional
    public void processPaymentWebhook(@NonNull String gatewayName, @NonNull Map<String, Object> payload, String signature) {
        log.info("Recebido webhook de pagamento do gateway: {}", gatewayName);

        PaymentGatewayType type;
        try {
            type = PaymentGatewayType.valueOf(gatewayName.toUpperCase().replace("-", "_"));
        } catch (Exception e) {
            type = PaymentGatewayType.XINGUBIT_PAY;
        }

        PaymentGatewayResolver.ResolvedGateway resolved = gatewayResolver.resolve(type);
        PaymentGateway gateway = resolved.gateway();

        // Validação e extração do txId pelo gateway específico
        String txId = gateway.processWebhook(payload, signature, resolved.config());

        // Idempotência por txId do Webhook
        idempotencyService.executeIdempotent(UuidCreatorUtils.generateUuidV7(), "Webhook-" + txId, () -> {
            Invoice invoice = invoiceService.getInvoiceByExternalTransactionId(txId)
                    .orElseThrow(() -> new RuntimeException("Fatura correspondente ao txId " + txId + " não encontrada"));

            BigDecimal paidAmount = payload.get("paidAmount") != null 
                    ? new BigDecimal(payload.get("paidAmount").toString()) 
                    : invoice.getAmount();

            invoiceService.markInvoiceAsPaid(invoice.getId(), paidAmount, "PIX");

            try {
                PaymentTransaction tx = PaymentTransaction.builder()
                        .id(UuidCreatorUtils.generateUuidV7())
                        .invoiceId(invoice.getId())
                        .gatewayType(gateway.getGatewayType().name())
                        .transactionType("WEBHOOK_NOTIFICATION")
                        .rawPayload(objectMapper.writeValueAsString(payload))
                        .status("PAID")
                        .build();
                transactionRepository.save(tx);
            } catch (Exception e) {
                log.error("Erro ao salvar log de transação do webhook: {}", e.getMessage());
            }

            log.info("Webhook processado com sucesso para fatura {} (txId={})", invoice.getId(), txId);
        });
    }
}
