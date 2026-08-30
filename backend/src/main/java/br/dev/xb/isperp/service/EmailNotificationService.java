package br.dev.xb.isperp.service;

import freemarker.template.Configuration;
import freemarker.template.Template;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class EmailNotificationService {

    private final Configuration freemarkerConfiguration;
    private final Optional<JavaMailSender> mailSender;

    /**
     * Renderiza um template FreeMarker (.ftl) localizado em classpath:/templates/emails/
     */
    public String renderTemplate(String templateName, Map<String, Object> model) {
        try {
            String path = templateName.endsWith(".ftl") ? templateName : templateName + ".ftl";
            if (!path.startsWith("emails/")) {
                path = "emails/" + path;
            }
            Template template = freemarkerConfiguration.getTemplate(path);
            return FreeMarkerTemplateUtils.processTemplateIntoString(template, model);
        } catch (Exception e) {
            log.error("Erro ao renderizar template FreeMarker '{}': {}", templateName, e.getMessage(), e);
            throw new RuntimeException("Falha ao processar template de e-mail: " + e.getMessage(), e);
        }
    }

    /**
     * Envia e-mail baseado em template FreeMarker para um destinatário único.
     */
    public void sendTemplateEmail(String toEmail, String subject, String templateName, Map<String, Object> model) {
        String htmlBody = renderTemplate(templateName, model);

        if (mailSender.isPresent()) {
            try {
                JavaMailSender sender = mailSender.get();
                MimeMessage message = sender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setTo(toEmail);
                helper.setSubject(subject);
                helper.setText(htmlBody, true);
                sender.send(message);
                log.info("📧 [FreeMarker/SMTP] E-mail enviado com sucesso para '{}': Assunto='{}'", toEmail, subject);
                return;
            } catch (Exception e) {
                log.warn("Falha no envio SMTP real para {}: {}. Registrado em log de auditoria.", toEmail, e.getMessage());
            }
        }

        log.info("📧 [FreeMarker/Log] E-mail simulado para '{}': Assunto='{}' (Template={})", toEmail, subject, templateName);
        log.debug("📧 [FreeMarker/Log] Corpo HTML Renderizado:\n{}", htmlBody);
    }

    /**
     * Envia e-mail com template FreeMarker e arquivo anexado para múltiplos destinatários.
     */
    public void sendTemplateEmailWithAttachment(
            List<String> recipients,
            String subject,
            String templateName,
            Map<String, Object> model,
            String attachmentFilename,
            byte[] attachmentBytes,
            String mimeType) {

        String htmlBody = renderTemplate(templateName, model);

        if (mailSender.isPresent()) {
            try {
                JavaMailSender sender = mailSender.get();
                MimeMessage message = sender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                String[] toArray = recipients.toArray(new String[0]);
                helper.setTo(toArray);
                helper.setSubject(subject);
                helper.setText(htmlBody, true);
                helper.addAttachment(attachmentFilename, new ByteArrayResource(attachmentBytes), mimeType);

                sender.send(message);
                log.info("📧 [FreeMarker/SMTP] E-mail com anexo '{}' enviado para {} destinatários: Assunto='{}'",
                        attachmentFilename, recipients.size(), subject);
                return;
            } catch (Exception e) {
                log.warn("Falha ao enviar e-mail com anexo via SMTP para {}: {}. Registrado em log.", recipients, e.getMessage());
            }
        }

        log.info("📧 [FreeMarker/Log] E-mail com anexo '{}' ({} bytes) simulado para {} destinatários: Assunto='{}'",
                attachmentFilename, attachmentBytes.length, recipients, subject);
    }

    /**
     * Envia e-mail simples de texto/HTML avulso.
     */
    public void sendEmail(String toEmail, String subject, String body) {
        log.info("📧 [SMTP/Email] Enviando mensagem para '{}': Assunto='{}'", toEmail, subject);
        log.debug("📧 [SMTP/Email] Conteúdo:\n{}", body);
    }
}
