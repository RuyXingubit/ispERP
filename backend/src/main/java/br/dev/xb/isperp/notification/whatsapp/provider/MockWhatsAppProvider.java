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
public class MockWhatsAppProvider implements WhatsAppProvider {

    @Override
    public WhatsAppProviderType getProviderType() {
        return WhatsAppProviderType.MOCK;
    }

    @Override
    public boolean sendTextMessage(@NonNull String toPhone, @NonNull String message, @NonNull NotificationConfig config) {
        log.info("MockWhatsApp: Simulação de envio para {}: {}", toPhone, message);
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
        log.info("MockWhatsApp: Simulação de envio de fatura Pix (R$ {}) para {}", amount, toPhone);
        return true;
    }
}
