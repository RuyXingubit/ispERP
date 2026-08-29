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
public class PaymentWebhookService {

    private final PaymentGatewayResolver gatewayResolver;
    private final InvoiceService invoiceService;
    private final PaymentTransactionRepository transactionRepository;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;
    private final NfcomDecisionService nfcomDecisionService;
    private final br.dev.xb.isperp.gateway.xingubit.XingubitPayGateway xingubitPayGateway;
    private final br.dev.xb.isperp.repository.CustomerRepository customerRepository;
    private final br.dev.xb.isperp.repository.InvoiceRepository invoiceRepository;

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

            // Emissão condicional de NFCom (Modelo 62) via Xingubit Pay
            if (nfcomDecisionService.shouldIssueNfcom(invoice)) {
                try {
                    var customerOpt = customerRepository.findById(invoice.getCustomerId());
                    String customerName = customerOpt.map(br.dev.xb.isperp.entity.Customer::getName).orElse("Cliente");
                    String customerCpfCnpj = customerOpt.map(br.dev.xb.isperp.entity.Customer::getCpf).orElse("00000000000");

                    var nfcomRes = xingubitPayGateway.issueNfcom(
                            txId, customerCpfCnpj, customerName, paidAmount, resolved.config());

                    invoice.setNfcomNumber(nfcomRes.getNfcomNumber());
                    invoice.setNfcomSeries(nfcomRes.getNfcomSeries());
                    invoice.setNfcomKey(nfcomRes.getNfcomKey());
                    invoice.setNfcomXmlUrl(nfcomRes.getXmlUrl());
                    invoice.setNfcomPdfUrl(nfcomRes.getPdfUrl());
                    invoice.setNfcomStatus(Invoice.NfcomStatus.ISSUED);
                    invoice.setNfcomIssuedAt(nfcomRes.getIssuedAt());
                    invoiceRepository.save(invoice);
                    log.info("NFCom emitida com sucesso para fatura {}. Chave={}", invoice.getId(), nfcomRes.getNfcomKey());
                } catch (Exception ex) {
                    log.error("Erro ao emitir NFCom para fatura {}: {}", invoice.getId(), ex.getMessage());
                    invoice.setNfcomStatus(Invoice.NfcomStatus.FAILED);
                    invoice.setNfcomErrorMessage(ex.getMessage());
                    invoiceRepository.save(invoice);
                }
            }

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
