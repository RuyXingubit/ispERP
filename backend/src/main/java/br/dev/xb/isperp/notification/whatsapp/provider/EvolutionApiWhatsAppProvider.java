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
public class EvolutionApiWhatsAppProvider implements WhatsAppProvider {

    @Override
    public WhatsAppProviderType getProviderType() {
        return WhatsAppProviderType.EVOLUTION_API;
    }

    @Override
    public boolean sendTextMessage(String toPhone, String message, NotificationConfig config) {
        String cleanPhone = toPhone.replaceAll("[^0-9]", "");
        log.info("EvolutionAPI: Enviando mensagem via Baileys/Evolution em {} para {}",
                config.getApiUrl() != null ? config.getApiUrl() : "http://localhost:8080", cleanPhone);
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
                "🔔 *Aviso de Fatura ISP ERP*\n\nOlá %s, sua fatura de R$ %s vence em %s.\n\nCódigo Pix Copia e Cola:\n%s",
                customerName, amount.toString().replace(".", ","), dueDate, pixCode
        );

        return sendTextMessage(toPhone, message, config);
    }
}
