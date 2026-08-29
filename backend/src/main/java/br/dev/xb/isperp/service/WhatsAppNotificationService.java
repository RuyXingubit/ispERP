package br.dev.xb.isperp.service;

import br.dev.xb.isperp.entity.NotificationConfig;
import br.dev.xb.isperp.entity.NotificationLog;
import br.dev.xb.isperp.notification.whatsapp.WhatsAppProvider;
import br.dev.xb.isperp.notification.whatsapp.WhatsAppProviderResolver;
import br.dev.xb.isperp.notification.whatsapp.WhatsAppProviderType;
import br.dev.xb.isperp.repository.NotificationLogRepository;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.lang.NonNull;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppNotificationService {

    private final WhatsAppProviderResolver providerResolver;
    private final NotificationLogRepository logRepository;

    /**
     * Envia cobrança com código Pix via WhatsApp usando o provedor ativo (Twilio, Evolution API, Z-API).
     */
    public boolean sendPixInvoice(
            @NonNull UUID customerId,
            String toPhone,
            String customerName,
            BigDecimal amount,
            String dueDate,
            String pixCode,
            String pixQrUrl) {

        log.info("WhatsAppNotification: Enviando Pix para {} (R$ {})", customerName, amount);

        WhatsAppProviderResolver.ResolvedWhatsAppProvider resolved = providerResolver.resolve(WhatsAppProviderType.TWILIO);
        WhatsAppProvider provider = resolved.provider();
        NotificationConfig config = resolved.config();

        boolean success = false;
        try {
            success = provider.sendPixInvoiceMessage(toPhone, customerName, amount, dueDate, pixCode, pixQrUrl, config);
        } catch (Exception e) {
            log.error("Erro ao enviar mensagem WhatsApp via {}: {}", provider.getProviderType(), e.getMessage());
        }

        // Salva log da notificação
        NotificationLog notificationLog = NotificationLog.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .customerId(customerId)
                .channel("WHATSAPP")
                .destination(toPhone)
                .messageType("INVOICE_PIX")
                .status(success ? "SENT" : "FAILED")
                .payload("Fatura R$ " + amount + " Vencimento " + dueDate)
                .build();
        logRepository.save(notificationLog);

        return success;
    }

    /**
     * Envia mensagem de texto genérica via WhatsApp.
     */
    public boolean sendTextMessage(@NonNull UUID customerId, String toPhone, String message, String messageType) {
        WhatsAppProviderResolver.ResolvedWhatsAppProvider resolved = providerResolver.resolve(WhatsAppProviderType.TWILIO);
        WhatsAppProvider provider = resolved.provider();
        NotificationConfig config = resolved.config();

        boolean success = false;
        try {
            success = provider.sendTextMessage(toPhone, message, config);
        } catch (Exception e) {
            log.error("Erro ao enviar mensagem WhatsApp: {}", e.getMessage());
        }

        NotificationLog notificationLog = NotificationLog.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .customerId(customerId)
                .channel("WHATSAPP")
                .destination(toPhone)
                .messageType(messageType != null ? messageType : "GENERAL_MESSAGE")
                .status(success ? "SENT" : "FAILED")
                .payload(message)
                .build();
        logRepository.save(notificationLog);

        return success;
    }
}
