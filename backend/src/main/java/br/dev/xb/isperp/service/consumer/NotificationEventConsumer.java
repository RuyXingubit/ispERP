package br.dev.xb.isperp.service.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import br.dev.xb.isperp.event.DomainEvent;
import br.dev.xb.isperp.service.EmailNotificationService;
import br.dev.xb.isperp.service.IdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventConsumer {

    private static final String CONSUMER_NAME = "MultiChannelNotificationConsumer";

    private final EmailNotificationService emailService;
    private final br.dev.xb.isperp.service.WhatsAppNotificationService whatsAppService;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    @Async("eventTaskExecutor")
    @EventListener
    public void handleDomainEvent(DomainEvent event) {
        String type = event.getEventType();

        if ("INVOICE_GENERATED".equals(type)) {
            handleInvoiceGenerated(event);
        } else if ("INVOICE_PAID".equals(type)) {
            handleInvoicePaid(event);
        } else if ("CLIENT_ACCESS_GENERATED".equals(type)) {
            handleClientAccessGenerated(event);
        } else if ("PLAN_UPGRADED".equals(type)) {
            handlePlanUpgraded(event);
        }
    }

    private void handleInvoiceGenerated(DomainEvent event) {
        idempotencyService.executeIdempotent(event.getEventId(), CONSUMER_NAME + "_InvoiceGen", () -> {
            Map<String, Object> data = extractPayload(event.getPayload());
            String email = (String) data.get("customerEmail");
            String phone = (String) data.get("customerPhone");
            String name = (String) data.get("customerName");
            String amount = (String) data.get("amount");
            String dueDate = (String) data.get("dueDate");
            String pixCode = (String) data.get("pixCopiaECola");
            String pixQrUrl = (String) data.get("pixQrCodeUrl");

            if (email != null && !email.isEmpty()) {
                String subject = "Sua Fatura de Internet chegou! Vencimento em " + dueDate;
                String body = String.format("Olá %s,\nSua fatura no valor de R$ %s com vencimento em %s já está disponível.\n\nPague via Pix Copia e Cola:\n%s",
                        name, amount, dueDate, pixCode);
                emailService.sendEmail(email, subject, body);
            }

            if (phone != null && !phone.isEmpty()) {
                java.math.BigDecimal val = amount != null ? new java.math.BigDecimal(amount) : java.math.BigDecimal.ZERO;
                whatsAppService.sendPixInvoice(null, phone, name != null ? name : "Cliente", val, dueDate != null ? dueDate : "", pixCode != null ? pixCode : "", pixQrUrl);
            }
        });
    }

    private void handleInvoicePaid(DomainEvent event) {
        idempotencyService.executeIdempotent(event.getEventId(), CONSUMER_NAME + "_InvoicePaid", () -> {
            Map<String, Object> data = extractPayload(event.getPayload());
            String invoiceId = (String) data.get("invoiceId");
            String paidAmount = (String) data.get("paidAmount");
            String phone = (String) data.get("customerPhone");

            log.info("Comprovante de pagamento emitido para fatura {} (R$ {})", invoiceId, paidAmount);

            if (phone != null && !phone.isEmpty()) {
                String msg = String.format("✅ *Pagamento Confirmado!*\n\nRecebemos o pagamento da sua fatura no valor de R$ %s. Sua conexão de internet está totalmente liberada. Obrigado!", paidAmount);
                whatsAppService.sendTextMessage(null, phone, msg, "PAYMENT_CONFIRMATION");
            }
        });
    }

    private void handleClientAccessGenerated(DomainEvent event) {
        idempotencyService.executeIdempotent(event.getEventId(), CONSUMER_NAME + "_AccessGen", () -> {
            Map<String, Object> data = extractPayload(event.getPayload());
            String email = (String) data.get("userEmail");
            String phone = (String) data.get("customerPhone");
            String username = (String) data.get("username");
            String initialPassword = (String) data.get("initialPassword");

            if (email != null && !email.isEmpty()) {
                String subject = "Bem-vindo ao ISP ERP - Seus dados de acesso";
                String body = String.format("Seu acesso à Central do Assinante foi criado!\nLogin: %s\nSenha Inicial: %s\nRecomendamos alterar a senha no primeiro acesso.",
                        username, initialPassword);
                emailService.sendEmail(email, subject, body);
            }

            if (phone != null && !phone.isEmpty()) {
                String msg = String.format("🚀 *Bem-vindo à nossa rede!*\n\nSeu acesso à Central do Assinante foi criado:\n👤 *Login:* %s\n🔑 *Senha:* %s\n\nAcesse para ver suas faturas e gerenciar seu plano!", username, initialPassword);
                whatsAppService.sendTextMessage(null, phone, msg, "WELCOME_CREDENTIALS");
            }
        });
    }

    private void handlePlanUpgraded(DomainEvent event) {
        idempotencyService.executeIdempotent(event.getEventId(), CONSUMER_NAME + "_PlanUpgraded", () -> {
            Map<String, Object> data = extractPayload(event.getPayload());
            String phone = (String) data.get("customerPhone");
            Object downloadSpeed = data.get("downloadSpeed");

            if (phone != null && !phone.isEmpty()) {
                String msg = String.format("⚡ *Upgrade de Plano Ativado!*\n\nSua velocidade foi atualizada para %s Mbps com sucesso. Aproveite sua conexão ultra-rápida!", downloadSpeed);
                whatsAppService.sendTextMessage(null, phone, msg, "PLAN_UPGRADED");
            }
        });
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractPayload(Object payload) {
        if (payload instanceof Map) {
            return (Map<String, Object>) payload;
        }
        try {
            return objectMapper.readValue(payload.toString(), new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Erro ao converter payload", e);
        }
    }
}
