package br.dev.xb.isperp.service;

import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class EmailNotificationServiceTest {

    private EmailNotificationService service;

    @BeforeEach
    void setUp() {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        cfg.setClassForTemplateLoading(this.getClass(), "/templates");
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);

        service = new EmailNotificationService(cfg, Optional.empty());
    }

    @Test
    @DisplayName("Deve renderizar template FreeMarker de fechamento fiscal para contabilidade")
    void testRenderAccountingTemplate() {
        Map<String, Object> model = new HashMap<>();
        model.put("accountingName", "Assessoria Contábil Xingu");
        model.put("empresa", Map.of(
                "razaoSocial", "Provedor Xingu Telecom Ltda",
                "cnpj", "12.345.678/0001-95",
                "inscricaoEstadual", "15999888"
        ));
        model.put("mesReferencia", "Agosto / 2026");
        model.put("totalFaturas", 150);
        model.put("totalFaturado", "14.985,00");
        model.put("totalIcms", "0,00");
        model.put("totalFust", "97,40");
        model.put("totalFunttel", "74,92");
        model.put("md5Mestre", "D41D8CD98F00B204E9800998ECF8427E");
        model.put("md5Item", "D41D8CD98F00B204E9800998ECF8427E");
        model.put("md5Destinatario", "D41D8CD98F00B204E9800998ECF8427E");
        model.put("nomeArquivoZip", "CONVENIO_115_2026_08.zip");

        String html = service.renderTemplate("fechamento_fiscal_contador", model);

        assertNotNull(html);
        assertTrue(html.contains("Assessoria Contábil Xingu"));
        assertTrue(html.contains("Provedor Xingu Telecom Ltda"));
        assertTrue(html.contains("14.985,00"));
        assertTrue(html.contains("CONVENIO_115_2026_08.zip"));
    }

    @Test
    @DisplayName("Deve renderizar template FreeMarker de nova fatura")
    void testRenderInvoiceTemplate() {
        Map<String, Object> model = Map.of(
                "customerName", "Carlos Eduardo",
                "planName", "Fibra 500 Mega",
                "dueDate", "10/09/2026",
                "amount", "99,90",
                "pixCopiaECola", "00020126580014br.gov.bcb.pix...",
                "portalUrl", "https://isperp.local/portal",
                "companyName", "Xingu Telecom"
        );

        String html = service.renderTemplate("fatura_emitida", model);

        assertNotNull(html);
        assertTrue(html.contains("Carlos Eduardo"));
        assertTrue(html.contains("Fibra 500 Mega"));
        assertTrue(html.contains("99,90"));
    }

    @Test
    @DisplayName("Deve renderizar template FreeMarker de código 2FA para upgrade PF")
    void testRenderUpgradeOtpTemplate() {
        Map<String, Object> model = Map.of(
                "customerName", "Maria Silva",
                "verificationCode", "984210",
                "targetPlanName", "Fibra Gamer 1000 Mega",
                "targetPlanPrice", "149,90",
                "requestDateTime", "29/08/2026 14:20"
        );

        String html = service.renderTemplate("codigo_verificacao_upgrade", model);

        assertNotNull(html);
        assertTrue(html.contains("984210"));
        assertTrue(html.contains("Fibra Gamer 1000 Mega"));
    }

    @Test
    @DisplayName("Deve renderizar template FreeMarker de Magic Link PJ")
    void testRenderPjMagicLinkTemplate() {
        Map<String, Object> model = Map.of(
                "companyName", "Supermercado Líder Ltda",
                "companyCnpj", "12.345.678/0001-95",
                "magicLinkUrl", "https://isperp.local/portal/auth?token=sec_token_123"
        );

        String html = service.renderTemplate("acesso_portal_pj", model);

        assertNotNull(html);
        assertTrue(html.contains("Supermercado Líder Ltda"));
        assertTrue(html.contains("sec_token_123"));
    }

    @Test
    @DisplayName("Deve renderizar template FreeMarker de chamado ANATEL")
    void testRenderAnatelTicketTemplate() {
        Map<String, Object> model = Map.of(
                "customerName", "João Pereira",
                "protocol", "20260829-001234",
                "category", "Lentidão",
                "subject", "Sem conexão fibra",
                "slaDeadline", "30/08/2026 18:00",
                "portalUrl", "https://isperp.local/portal"
        );

        String html = service.renderTemplate("chamado_anatel", model);

        assertNotNull(html);
        assertTrue(html.contains("20260829-001234"));
        assertTrue(html.contains("João Pereira"));
    }

    @Test
    @DisplayName("Deve renderizar template FreeMarker de Desbloqueio em Confiança 48h")
    void testRenderTrustUnblockTemplate() {
        Map<String, Object> model = Map.of(
                "customerName", "Roberto Souza",
                "expirationDateTime", "31/08/2026 14:00",
                "portalUrl", "https://isperp.local/portal"
        );

        String html = service.renderTemplate("desbloqueio_confianca", model);

        assertNotNull(html);
        assertTrue(html.contains("Roberto Souza"));
        assertTrue(html.contains("31/08/2026 14:00"));
    }
}
