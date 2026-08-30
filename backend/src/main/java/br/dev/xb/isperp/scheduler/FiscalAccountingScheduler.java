package br.dev.xb.isperp.scheduler;

import br.dev.xb.isperp.entity.FiscalCompany;
import br.dev.xb.isperp.repository.FiscalCompanyRepository;
import br.dev.xb.isperp.service.ConvenioIcms115Service;
import br.dev.xb.isperp.service.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class FiscalAccountingScheduler {

    private final FiscalCompanyRepository companyRepository;
    private final ConvenioIcms115Service convenio115Service;
    private final EmailNotificationService emailNotificationService;

    /**
     * Executa diariamente às 08:00 para verificar se há empresas com fechamento fiscal agendado para hoje.
     */
    @Scheduled(cron = "${fiscal.accounting.cron:0 0 8 * * *}")
    public void executeScheduledAccountingDispatch() {
        LocalDate today = LocalDate.now();
        int currentDay = today.getDayOfMonth();
        log.info("⏰ [FiscalScheduler] Verificando empresas para envio automático do Convênio 115/03 (Dia {})", currentDay);

        List<FiscalCompany> companies = companyRepository.findAll().stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
                .filter(c -> Boolean.TRUE.equals(c.getAccountingAutoSend()))
                .filter(c -> c.getAccountingEmails() != null && !c.getAccountingEmails().isBlank())
                .filter(c -> c.getAccountingSendDay() != null && c.getAccountingSendDay() == currentDay)
                .toList();

        for (FiscalCompany company : companies) {
            try {
                sendMonthlyReportToAccounting(company, today.minusMonths(1).getYear(), today.minusMonths(1).getMonthValue());
            } catch (Exception e) {
                log.error("Erro ao enviar relatório fiscal automático para {}: {}", company.getRazaoSocial(), e.getMessage(), e);
            }
        }
    }

    /**
     * Gera o lote do mês/ano informado e envia por e-mail para a assessoria contábil.
     */
    public boolean sendMonthlyReportToAccounting(FiscalCompany company, int year, int month) {
        log.info("📧 Gerando e enviando fechamento fiscal ({}/{}) para a contabilidade de '{}'",
                month, year, company.getRazaoSocial());

        ConvenioIcms115Service.Convenio115BatchResult batch = convenio115Service.generateMonthlyBatch(company.getId(), year, month);

        List<String> recipients = Arrays.stream(company.getAccountingEmails().split(","))
                .map(String::trim)
                .filter(e -> !e.isBlank())
                .toList();

        if (recipients.isEmpty()) {
            log.warn("Nenhum e-mail de contabilidade configurado para a empresa {}", company.getRazaoSocial());
            return false;
        }

        LocalDate refDate = LocalDate.of(year, month, 1);
        String mesReferencia = refDate.getMonth().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("pt-BR")) + " / " + year;
        String zipFilename = String.format("CONVENIO_115_%s_%d_%02d.zip",
                company.getCnpj().replaceAll("[^0-9]", ""), year, month);

        Map<String, Object> model = new HashMap<>();
        model.put("accountingName", company.getAccountingName() != null ? company.getAccountingName() : "Assessoria Contábil");
        model.put("empresa", company);
        model.put("mesReferencia", mesReferencia);
        model.put("totalFaturas", batch.getTotalRecords());
        model.put("totalFaturado", String.format(Locale.GERMAN, "%,.2f", batch.getTotalFaturado()));
        model.put("totalIcms", String.format(Locale.GERMAN, "%,.2f", batch.getTotalIcms()));
        model.put("totalFust", String.format(Locale.GERMAN, "%,.2f", batch.getTotalFaturado().multiply(new BigDecimal("0.0065"))));
        model.put("totalFunttel", String.format(Locale.GERMAN, "%,.2f", batch.getTotalFaturado().multiply(new BigDecimal("0.0050"))));
        model.put("md5Mestre", batch.getMd5Mestre());
        model.put("md5Item", batch.getMd5Item());
        model.put("md5Destinatario", batch.getMd5Destinatario());
        model.put("nomeArquivoZip", zipFilename);

        String subject = String.format("Fechamento Fiscal - Convênio ICMS 115/03 - %s (%s)",
                mesReferencia, company.getRazaoSocial());

        emailNotificationService.sendTemplateEmailWithAttachment(
                recipients,
                subject,
                "fechamento_fiscal_contador",
                model,
                zipFilename,
                batch.getZipBytes(),
                "application/zip"
        );

        company.setAccountingLastSentAt(LocalDateTime.now());
        companyRepository.save(company);

        log.info("✅ Fechamento fiscal enviado com sucesso para {} com anexo '{}'", recipients, zipFilename);
        return true;
    }
}
