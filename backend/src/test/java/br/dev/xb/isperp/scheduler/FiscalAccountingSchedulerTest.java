package br.dev.xb.isperp.scheduler;

import br.dev.xb.isperp.entity.FiscalCompany;
import br.dev.xb.isperp.repository.FiscalCompanyRepository;
import br.dev.xb.isperp.service.ConvenioIcms115Service;
import br.dev.xb.isperp.service.EmailNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FiscalAccountingSchedulerTest {

    private FiscalCompanyRepository companyRepository;
    private ConvenioIcms115Service convenio115Service;
    private EmailNotificationService emailNotificationService;
    private FiscalAccountingScheduler scheduler;

    @BeforeEach
    void setUp() {
        companyRepository = Mockito.mock(FiscalCompanyRepository.class);
        convenio115Service = Mockito.mock(ConvenioIcms115Service.class);
        emailNotificationService = Mockito.mock(EmailNotificationService.class);

        scheduler = new FiscalAccountingScheduler(companyRepository, convenio115Service, emailNotificationService);
    }

    @Test
    @DisplayName("Deve gerar lote e enviar por e-mail com anexo ZIP para os contadores cadastrados")
    void testSendMonthlyReportToAccounting() {
        UUID companyId = UUID.randomUUID();
        FiscalCompany company = FiscalCompany.builder()
                .id(companyId)
                .cnpj("12.345.678/0001-95")
                .razaoSocial("Provedor Xingu Telecom")
                .inscricaoEstadual("15999888")
                .accountingName("Contabilidade Silva & Associados")
                .accountingEmails("fiscal@contabilidadesilva.com.br, socio@contabilidadesilva.com.br")
                .accountingSendDay(5)
                .accountingAutoSend(true)
                .build();

        ConvenioIcms115Service.Convenio115BatchResult batch = ConvenioIcms115Service.Convenio115BatchResult.builder()
                .totalRecords(10)
                .totalFaturado(new BigDecimal("999.00"))
                .totalIcms(BigDecimal.ZERO)
                .md5Mestre("D41D8CD98F00B204E9800998ECF8427E")
                .md5Item("D41D8CD98F00B204E9800998ECF8427E")
                .md5Destinatario("D41D8CD98F00B204E9800998ECF8427E")
                .zipBytes("FAKE_ZIP_DATA".getBytes())
                .build();

        when(convenio115Service.generateMonthlyBatch(eq(companyId), eq(2026), eq(8))).thenReturn(batch);

        boolean sent = scheduler.sendMonthlyReportToAccounting(company, 2026, 8);

        assertTrue(sent);
        verify(emailNotificationService, times(1)).sendTemplateEmailWithAttachment(
                eq(List.of("fiscal@contabilidadesilva.com.br", "socio@contabilidadesilva.com.br")),
                anyString(),
                eq("fechamento_fiscal_contador"),
                anyMap(),
                anyString(),
                eq("FAKE_ZIP_DATA".getBytes()),
                eq("application/zip")
        );
        verify(companyRepository, times(1)).save(company);
        assertNotNull(company.getAccountingLastSentAt());
    }
}
