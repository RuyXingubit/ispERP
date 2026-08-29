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
public class EvolutionApiWhatsAppProvider implements WhatsAppProvider {

    @Override
    public WhatsAppProviderType getProviderType() {
        return WhatsAppProviderType.EVOLUTION_API;
    }

    @Override
    public boolean sendTextMessage(@NonNull String toPhone, @NonNull String message, @NonNull NotificationConfig config) {
        String cleanPhone = toPhone.replaceAll("[^0-9]", "");
        log.info("EvolutionAPI: Enviando mensagem via Baileys/Evolution em {} para {}",
                config.getApiUrl() != null ? config.getApiUrl() : "http://localhost:8080", cleanPhone);
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
                "🔔 *Aviso de Fatura ISP ERP*\n\nOlá %s, sua fatura de R$ %s vence em %s.\n\nCódigo Pix Copia e Cola:\n%s",
                customerName, amount.toString().replace(".", ","), dueDate, pixCode
        );

        return sendTextMessage(toPhone, message, config);
    }
}
