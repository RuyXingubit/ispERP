package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.contract.SignatureWebhooksApi;
import br.dev.xb.isperp.api.dto.FallbackMethod;
import br.dev.xb.isperp.api.dto.PixSignatureWebhookRequest;
import br.dev.xb.isperp.api.dto.SignatureSessionResponse;
import br.dev.xb.isperp.api.dto.SignatureStatus;
import br.dev.xb.isperp.service.ElectronicSignatureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@SuppressWarnings("null")
public class ElectronicSignatureWebhookController implements SignatureWebhooksApi {

    private final ElectronicSignatureService signatureService;

    @Override
    @PostMapping({"/webhooks/signatures/pix", "/api/webhooks/signatures/pix"})
    public ResponseEntity<SignatureSessionResponse> handlePixSignatureWebhook(@RequestBody PixSignatureWebhookRequest request) {
        log.info("Recebida notificação webhook Pix de assinatura: TxID={}", request.getTxid());
        br.dev.xb.isperp.dto.PixSignatureWebhookRequest internalReq = br.dev.xb.isperp.dto.PixSignatureWebhookRequest.builder()
                .txid(request.getTxid())
                .endToEndId(request.getEndToEndId())
                .amount(request.getAmount() != null ? BigDecimal.valueOf(request.getAmount()) : null)
                .payerName(request.getPayerName())
                .payerCpfCnpj(request.getPayerCpfCnpj())
                .bankName(request.getBankName())
                .ispb(request.getIspb())
                .build();

        br.dev.xb.isperp.dto.SignatureSessionResponse internalRes = signatureService.processPixSignatureWebhook(internalReq);
        return ResponseEntity.ok(toApiResponse(internalRes));
    }

    private SignatureSessionResponse toApiResponse(br.dev.xb.isperp.dto.SignatureSessionResponse internal) {
        if (internal == null) return null;
        SignatureSessionResponse res = new SignatureSessionResponse();
        res.setId(internal.getId());
        res.setContractId(internal.getContractId());
        res.setTemplateId(internal.getTemplateId());
        res.setToken(internal.getToken());
        res.setSignatureUrl(internal.getSignatureUrl());
        if (internal.getStatus() != null) {
            res.setStatus(SignatureStatus.fromValue(internal.getStatus().name()));
        }
        if (internal.getSymbolicAmount() != null) {
            res.setSymbolicAmount(internal.getSymbolicAmount().doubleValue());
        }
        res.setPixTxid(internal.getPixTxid());
        res.setPixCopyPaste(internal.getPixCopyPaste());
        res.setPixQrCodeBase64(internal.getPixQrCodeBase64());
        res.setPixEndToEndId(internal.getPixEndToEndId());
        res.setDocumentSha256Hash(internal.getDocumentSha256Hash());
        res.setPayerName(internal.getPayerName());
        res.setPayerCpfCnpj(internal.getPayerCpfCnpj());
        res.setPayerBankName(internal.getPayerBankName());
        res.setRejectionReason(internal.getRejectionReason());
        res.setSignedPdfUrl(internal.getSignedPdfUrl());
        if (internal.getFallbackMethod() != null) {
            res.setFallbackMethod(FallbackMethod.fromValue(internal.getFallbackMethod().name()));
        }
        if (internal.getOnboardingCreditAmount() != null) {
            res.setOnboardingCreditAmount(internal.getOnboardingCreditAmount().doubleValue());
        }
        res.setDiscountAppliedInvoiceId(internal.getDiscountAppliedInvoiceId());
        res.setForensicCertificatePdfUrl(internal.getForensicCertificatePdfUrl());
        res.setExpiresAt(internal.getExpiresAt());
        res.setSignedAt(internal.getSignedAt());
        res.setCreatedAt(internal.getCreatedAt());
        return res;
    }
}
