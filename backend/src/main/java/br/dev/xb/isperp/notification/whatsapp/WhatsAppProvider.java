package br.dev.xb.isperp.notification.whatsapp;

import br.dev.xb.isperp.entity.NotificationConfig;
import org.springframework.lang.NonNull;

import java.math.BigDecimal;

public interface WhatsAppProvider {

    /**
     * Identificador do provedor de WhatsApp.
     */
    WhatsAppProviderType getProviderType();

    /**
     * Envia mensagem de texto simples.
     */
    boolean sendTextMessage(@NonNull String toPhone, @NonNull String message, @NonNull NotificationConfig config);

    /**
     * Envia mensagem com dados de fatura Pix (Valor, Vencimento, Copia e Cola e QR Code).
     */
    boolean sendPixInvoiceMessage(
            @NonNull String toPhone,
            @NonNull String customerName,
            @NonNull BigDecimal amount,
            @NonNull String dueDate,
            @NonNull String pixCode,
            String pixQrUrl,
            @NonNull NotificationConfig config
    );
}
