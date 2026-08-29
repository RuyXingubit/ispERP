package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.entity.FiscalCompany;
import br.dev.xb.isperp.entity.FiscalGatewayConfig;
import br.dev.xb.isperp.entity.NfcomRecord;
import br.dev.xb.isperp.fiscal.dto.CertificateUploadResult;
import br.dev.xb.isperp.fiscal.dto.NfcomCancelResult;
import br.dev.xb.isperp.repository.FiscalCompanyRepository;
import br.dev.xb.isperp.repository.FiscalGatewayConfigRepository;
import br.dev.xb.isperp.service.ConvenioIcms115Service;
import br.dev.xb.isperp.service.FiscalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/fiscal")
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class FiscalController {

    private final FiscalService fiscalService;
    private final ConvenioIcms115Service convenio115Service;
    private final FiscalCompanyRepository companyRepository;
    private final FiscalGatewayConfigRepository configRepository;
    private final br.dev.xb.isperp.scheduler.FiscalAccountingScheduler fiscalAccountingScheduler;

    @GetMapping("/company")
    public ResponseEntity<FiscalCompany> getActiveCompany() {
        return companyRepository.findFirstByIsActiveTrue()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/company")
    public ResponseEntity<FiscalCompany> saveCompany(@RequestBody FiscalCompany company) {
        FiscalCompany saved = fiscalService.saveCompany(company, null);
        return ResponseEntity.ok(saved);
    }

    @PostMapping(value = "/company/{companyId}/certificate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CertificateUploadResult> uploadCertificate(
            @PathVariable UUID companyId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("password") String password) {
        try {
            CertificateUploadResult result = fiscalService.uploadCertificate(companyId, file.getBytes(), password);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Erro ao processar upload do certificado: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(CertificateUploadResult.builder()
                    .success(false)
                    .errorMessage("Falha ao ler arquivo do certificado: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/configs")
    public ResponseEntity<FiscalGatewayConfig> getActiveConfig() {
        return configRepository.findFirstByIsActiveTrue()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(FiscalGatewayConfig.builder()
                        .companyId(UUID.randomUUID())
                        .baseUrl("https://pay.xingubit.com.br")
                        .environment("HOMOLOGACAO")
                        .build()));
    }

    @PostMapping("/invoices/{invoiceId}/emit")
    public ResponseEntity<NfcomRecord> emitNfcom(@PathVariable UUID invoiceId) {
        NfcomRecord record = fiscalService.issueNfcomForInvoice(invoiceId);
        return ResponseEntity.ok(record);
    }

    @GetMapping("/records")
    public ResponseEntity<Page<NfcomRecord>> listRecords(Pageable pageable) {
        return ResponseEntity.ok(fiscalService.listRecords(pageable));
    }

    @PostMapping("/records/{recordId}/cancel")
    public ResponseEntity<NfcomCancelResult> cancelNfcom(
            @PathVariable UUID recordId,
            @RequestBody Map<String, String> body) {
        String reason = body.getOrDefault("reason", "Cancelamento solicitado pelo provedor");
        return ResponseEntity.ok(fiscalService.cancelNfcom(recordId, reason));
    }

    @GetMapping("/convenio115/export")
    public ResponseEntity<byte[]> exportConvenio115(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        int y = (year != null) ? year : LocalDate.now().getYear();
        int m = (month != null) ? month : LocalDate.now().getMonthValue();

        ConvenioIcms115Service.Convenio115BatchResult result = convenio115Service.generateMonthlyBatch(null, y, m);

        String zipFilename = String.format("CONVENIO_115_%d_%02d.zip", y, m);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + zipFilename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(result.getZipBytes());
    }

    @PostMapping("/convenio115/send-accounting")
    public ResponseEntity<Map<String, Object>> sendAccountingReport(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        int y = (year != null) ? year : LocalDate.now().getYear();
        int m = (month != null) ? month : LocalDate.now().getMonthValue();

        FiscalCompany company = companyRepository.findFirstByIsActiveTrue()
                .orElseThrow(() -> new RuntimeException("Empresa fiscal não encontrada"));

        boolean sent = fiscalAccountingScheduler.sendMonthlyReportToAccounting(company, y, m);

        return ResponseEntity.ok(Map.of(
                "success", sent,
                "message", sent ? "Fechamento fiscal transmitido com sucesso para a contabilidade via FreeMarker!" : "Nenhum destinatário de contabilidade configurado.",
                "sentAt", java.time.LocalDateTime.now()
        ));
    }
}
