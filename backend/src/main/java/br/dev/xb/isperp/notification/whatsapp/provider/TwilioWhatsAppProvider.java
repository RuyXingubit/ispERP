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
public class TwilioWhatsAppProvider implements WhatsAppProvider {

    @Override
    public WhatsAppProviderType getProviderType() {
        return WhatsAppProviderType.TWILIO;
    }

    @Override
    public boolean sendTextMessage(String toPhone, String message, NotificationConfig config) {
        String formattedPhone = formatTwilioPhone(toPhone);
        String fromPhone = config.getFromPhoneNumber() != null ? config.getFromPhoneNumber() : "whatsapp:+14155238886";

        log.info("TwilioWhatsApp: Enviando mensagem para {} via AccountSID={}. From: {}",
                formattedPhone, config.getAccountSid(), fromPhone);
        log.debug("TwilioWhatsApp Conteúdo: {}", message);
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
                "Olá, *%s*! 👋\n\nSua fatura de internet no valor de *R$ %s* com vencimento em *%s* já está disponível.\n\n" +
                "📱 *Pix Copia e Cola:*\n```%s```\n\n" +
                "Ou pague escaneando o QR Code na Central do Assinante!",
                customerName, amount.toString().replace(".", ","), dueDate, pixCode
        );

        return sendTextMessage(toPhone, message, config);
    }

    private String formatTwilioPhone(String phone) {
        String clean = phone.replaceAll("[^0-9]", "");
        if (!clean.startsWith("55")) {
            clean = "55" + clean;
        }
        return "whatsapp:+" + clean;
    }
}
