package br.dev.xb.isperp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailNotificationService {

    /**
     * Envia e-mail de notificação (SMTP).
     */
    public void sendEmail(String toEmail, String subject, String body) {
        log.info("📧 [SMTP/Email] Enviando mensagem para '{}': Assunto='{}'", toEmail, subject);
        log.debug("📧 [SMTP/Email] Conteúdo:\n{}", body);
    }
}
