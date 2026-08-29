package br.dev.xb.isperp.notification.whatsapp;

import br.dev.xb.isperp.entity.NotificationConfig;
import br.dev.xb.isperp.notification.whatsapp.provider.TwilioWhatsAppProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TwilioWhatsAppProviderTest {

    private TwilioWhatsAppProvider twilioProvider;
    private NotificationConfig config;

    @BeforeEach
    void setUp() {
        twilioProvider = new TwilioWhatsAppProvider();
        config = NotificationConfig.builder()
                .name("Twilio Test")
                .providerType(WhatsAppProviderType.TWILIO)
                .accountSid("AC_test_12345")
                .authToken("token_secret_12345")
                .fromPhoneNumber("whatsapp:+14155238886")
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Deve identificar provedor como TWILIO")
    void shouldIdentifyAsTwilio() {
        assertEquals(WhatsAppProviderType.TWILIO, twilioProvider.getProviderType());
    }

    @Test
    @DisplayName("Deve enviar mensagem de texto com sucesso")
    void shouldSendTextMessage() {
        boolean sent = twilioProvider.sendTextMessage("11999998888", "Teste de mensagem", config);
        assertTrue(sent);
    }

    @Test
    @DisplayName("Deve formatar e enviar mensagem de Pix Copia e Cola via WhatsApp")
    void shouldSendPixInvoiceMessage() {
        boolean sent = twilioProvider.sendPixInvoiceMessage(
                "11999998888",
                "Ruy Barbosa",
                new BigDecimal("149.90"),
                "10/09/2026",
                "00020126580014br.gov.bcb.pix...",
                "https://api.qrserver.com/v1/create-qr-code/?data=pix",
                config
        );
        assertTrue(sent);
    }
}
