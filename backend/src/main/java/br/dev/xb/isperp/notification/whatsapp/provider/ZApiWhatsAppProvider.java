package br.dev.xb.isperp.notification.whatsapp.provider;

import br.dev.xb.isperp.entity.NotificationConfig;
import br.dev.xb.isperp.notification.whatsapp.WhatsAppProvider;
import br.dev.xb.isperp.notification.whatsapp.WhatsAppProviderType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Slf4j
public class ZApiWhatsAppProvider implements WhatsAppProvider {

    @Override
    public WhatsAppProviderType getProviderType() {
        return WhatsAppProviderType.Z_API;
    }

    @Override
    public boolean sendTextMessage(String toPhone, String message, NotificationConfig config) {
        String cleanPhone = toPhone.replaceAll("[^0-9]", "");
        log.info("Z-API: Enviando mensagem para {} via Token={}", cleanPhone, config.getApiToken());
        return true;
    }

    @Override
    public boolean sendPixInvoiceMessage(
            String toPhone,
            String customerName,
            BigDecimal amount,
            String dueDate,
            String pixCode,
            String pixQrUrl,
            NotificationConfig config) {

        String message = String.format(
                "Olá %s! Sua fatura de R$ %s com vencimento em %s está disponível. Pague via Pix: %s",
                customerName, amount, dueDate, pixCode
        );

        return sendTextMessage(toPhone, message, config);
    }
}
