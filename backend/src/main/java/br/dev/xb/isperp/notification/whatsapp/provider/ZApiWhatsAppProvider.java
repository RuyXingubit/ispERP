package br.dev.xb.isperp.notification.whatsapp.provider;

import br.dev.xb.isperp.entity.NotificationConfig;
import br.dev.xb.isperp.notification.whatsapp.WhatsAppProvider;
import br.dev.xb.isperp.notification.whatsapp.WhatsAppProviderType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Slf4j
@SuppressWarnings("null")
public class ZApiWhatsAppProvider implements WhatsAppProvider {

    @Override
    public WhatsAppProviderType getProviderType() {
        return WhatsAppProviderType.Z_API;
    }

    @Override
    public boolean sendTextMessage(@NonNull String toPhone, @NonNull String message, @NonNull NotificationConfig config) {
        String cleanPhone = toPhone.replaceAll("[^0-9]", "");
        log.info("Z-API: Enviando mensagem para {} via Token={}", cleanPhone, config.getApiToken());
        return true;
    }

    @Override
    public boolean sendPixInvoiceMessage(
            @NonNull String toPhone,
            @NonNull String customerName,
            @NonNull BigDecimal amount,
            @NonNull String dueDate,
            @NonNull String pixCode,
            String pixQrUrl,
            @NonNull NotificationConfig config) {

        String message = String.format(
                "Olá %s! Sua fatura de R$ %s com vencimento em %s está disponível. Pague via Pix: %s",
                customerName, amount, dueDate, pixCode
        );

        return sendTextMessage(toPhone, message, config);
    }
}
