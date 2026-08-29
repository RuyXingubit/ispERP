package br.dev.xb.isperp.notification.whatsapp;

import br.dev.xb.isperp.entity.NotificationConfig;

import java.math.BigDecimal;

public interface WhatsAppProvider {

    /**
     * Identificador do provedor de WhatsApp.
     */
    WhatsAppProviderType getProviderType();

    /**
     * Envia mensagem de texto simples.
     */
    boolean sendTextMessage(String toPhone, String message, NotificationConfig config);

    /**
     * Envia mensagem com dados de fatura Pix (Valor, Vencimento, Copia e Cola e QR Code).
     */
    boolean sendPixInvoiceMessage(
            String toPhone,
            String customerName,
            BigDecimal amount,
            String dueDate,
            String pixCode,
            String pixQrUrl,
            NotificationConfig config
    );
}
