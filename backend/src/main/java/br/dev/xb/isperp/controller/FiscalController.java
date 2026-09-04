package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.contract.FiscalApi;
import br.dev.xb.isperp.api.dto.*;
import br.dev.xb.isperp.entity.FiscalCompany;
import br.dev.xb.isperp.entity.FiscalGatewayConfig;
import br.dev.xb.isperp.entity.NfcomRecord;
import br.dev.xb.isperp.fiscal.dto.CertificateUploadResult;
import br.dev.xb.isperp.fiscal.dto.NfcomCancelResult;
import br.dev.xb.isperp.mapper.FiscalMapper;
import br.dev.xb.isperp.repository.FiscalCompanyRepository;
import br.dev.xb.isperp.repository.FiscalGatewayConfigRepository;
import br.dev.xb.isperp.scheduler.FiscalAccountingScheduler;
import br.dev.xb.isperp.service.ConvenioIcms115Service;
import br.dev.xb.isperp.service.FiscalService;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@SuppressWarnings("null")
public class FiscalController implements FiscalApi {

    private final FiscalService fiscalService;
    private final ConvenioIcms115Service convenio115Service;
    private final FiscalCompanyRepository companyRepository;
    private final FiscalGatewayConfigRepository configRepository;
    private final FiscalAccountingScheduler fiscalAccountingScheduler;
    private final FiscalMapper fiscalMapper;

    @Override
    public ResponseEntity<FiscalCompanyResponse> getActiveFiscalCompany() {
        return companyRepository.findFirstByIsActiveTrue()
                .map(company -> ResponseEntity.ok(fiscalMapper.toResponse(company)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<FiscalCompanyResponse> saveFiscalCompany(FiscalCompanySaveRequest fiscalCompanySaveRequest) {
        FiscalCompany companyToSave = fiscalMapper.toEntity(fiscalCompanySaveRequest);
        if (companyToSave.getId() == null) {
            companyRepository.findFirstByIsActiveTrue().ifPresent(existing -> companyToSave.setId(existing.getId()));
        }
        FiscalCompany saved = fiscalService.saveCompany(companyToSave, null);
        return ResponseEntity.ok(fiscalMapper.toResponse(saved));
    }

    @Override
    public ResponseEntity<CertificateUploadResultResponse> uploadFiscalCertificate(
            UUID companyId,
            MultipartFile file,
            String password) {
        try {
            CertificateUploadResult result = fiscalService.uploadCertificate(companyId, file.getBytes(), password);
            return ResponseEntity.ok(fiscalMapper.toCertificateUploadResponse(result));
        } catch (Exception e) {
            log.error("Erro ao processar upload do certificado: {}", e.getMessage(), e);
            CertificateUploadResultResponse errorResponse = new CertificateUploadResultResponse(false);
            errorResponse.setErrorMessage("Falha ao ler arquivo do certificado: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @Override
    public ResponseEntity<FiscalGatewayConfigResponse> getActiveFiscalConfig() {
        return configRepository.findFirstByIsActiveTrue()
                .map(config -> ResponseEntity.ok(fiscalMapper.toConfigResponse(config)))
                .orElseGet(() -> {
                    FiscalGatewayConfig defaultConfig = FiscalGatewayConfig.builder()
                            .companyId(UuidCreatorUtils.generateUuidV7())
                            .baseUrl("https://pay.xingubit.com.br")
                            .environment("HOMOLOGACAO")
                            .isActive(true)
                            .build();
                    return ResponseEntity.ok(fiscalMapper.toConfigResponse(defaultConfig));
                });
    }

    @Override
    public ResponseEntity<NfcomRecordResponse> emitNfcom(UUID invoiceId) {
        NfcomRecord record = fiscalService.issueNfcomForInvoice(invoiceId);
        return ResponseEntity.ok(fiscalMapper.toNfcomRecordResponse(record));
    }

    @Override
    public ResponseEntity<NfcomRecordsPageResponse> listNfcomRecords(Integer page, Integer size, String sort) {
        int pageNumber = (page != null) ? page : 0;
        int pageSize = (size != null) ? size : 10;
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<NfcomRecord> recordsPage = fiscalService.listRecords(pageable);
        return ResponseEntity.ok(fiscalMapper.toPageResponse(recordsPage));
    }

    @Override
    public ResponseEntity<NfcomCancelResultResponse> cancelNfcom(UUID recordId, NfcomCancelRequest nfcomCancelRequest) {
        String reason = (nfcomCancelRequest != null && nfcomCancelRequest.getReason() != null)
                ? nfcomCancelRequest.getReason()
                : "Cancelamento solicitado pelo provedor";
        NfcomCancelResult result = fiscalService.cancelNfcom(recordId, reason);
        return ResponseEntity.ok(fiscalMapper.toCancelResponse(result));
    }

    @Override
    public ResponseEntity<Resource> exportConvenio115(Integer year, Integer month) {
        int y = (year != null) ? year : LocalDate.now().getYear();
        int m = (month != null) ? month : LocalDate.now().getMonthValue();

        ConvenioIcms115Service.Convenio115BatchResult result = convenio115Service.generateMonthlyBatch(null, y, m);

        String zipFilename = String.format("CONVENIO_115_%d_%02d.zip", y, m);
        ByteArrayResource resource = new ByteArrayResource(result.getZipBytes());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + zipFilename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @Override
    public ResponseEntity<SendAccountingReportResponse> sendAccountingReport(Integer year, Integer month) {
        int y = (year != null) ? year : LocalDate.now().getYear();
        int m = (month != null) ? month : LocalDate.now().getMonthValue();

        FiscalCompany company = companyRepository.findFirstByIsActiveTrue()
                .orElseThrow(() -> new RuntimeException("Empresa fiscal não encontrada"));

        boolean sent = fiscalAccountingScheduler.sendMonthlyReportToAccounting(company, y, m);

        SendAccountingReportResponse response = new SendAccountingReportResponse(
                sent,
                sent ? "Fechamento fiscal transmitido com sucesso para a contabilidade via FreeMarker!" : "Nenhum destinatário de contabilidade configurado.",
                OffsetDateTime.now()
        );
        return ResponseEntity.ok(response);
    }
}
