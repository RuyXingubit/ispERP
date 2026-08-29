package br.dev.xb.isperp.notification.whatsapp.provider;

import br.dev.xb.isperp.entity.NotificationConfig;
import br.dev.xb.isperp.notification.whatsapp.WhatsAppProvider;
import br.dev.xb.isperp.notification.whatsapp.WhatsAppProviderType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Slf4j
@SuppressWarnings("null")
public class MockWhatsAppProvider implements WhatsAppProvider {

    @Override
    public WhatsAppProviderType getProviderType() {
        return WhatsAppProviderType.MOCK;
    }

    @Override
    public boolean sendTextMessage(String toPhone, String message, NotificationConfig config) {
        log.info("MockWhatsApp: Simulação de envio para {}: {}", toPhone, message);
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
        log.info("MockWhatsApp: Simulação de envio de fatura Pix (R$ {}) para {}", amount, toPhone);
        return true;
    }
}
