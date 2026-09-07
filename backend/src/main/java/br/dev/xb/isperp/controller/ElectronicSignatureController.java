package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.contract.SignaturesApi;
import br.dev.xb.isperp.api.dto.CreateSignatureSessionRequest;
import br.dev.xb.isperp.api.dto.FallbackMethod;
import br.dev.xb.isperp.api.dto.FallbackSelectionRequest;
import br.dev.xb.isperp.api.dto.SignaturePublicViewResponse;
import br.dev.xb.isperp.api.dto.SignatureSessionResponse;
import br.dev.xb.isperp.api.dto.SignatureStatus;
import br.dev.xb.isperp.service.ElectronicSignatureService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@SuppressWarnings("null")
public class ElectronicSignatureController implements SignaturesApi {

    private final ElectronicSignatureService signatureService;
    private final HttpServletRequest servletRequest;

    @Override
    @PostMapping({"/contracts/signatures", "/api/contracts/signatures"})
    public ResponseEntity<SignatureSessionResponse> createSignatureSession(@RequestBody CreateSignatureSessionRequest request) {
        String baseUrl = servletRequest.getScheme() + "://" + servletRequest.getServerName() +
                (servletRequest.getServerPort() != 80 && servletRequest.getServerPort() != 443 ? ":" + servletRequest.getServerPort() : "");

        br.dev.xb.isperp.dto.CreateSignatureSessionRequest internalReq = br.dev.xb.isperp.dto.CreateSignatureSessionRequest.builder()
                .contractId(request.getContractId())
                .templateId(request.getTemplateId())
                .symbolicAmount(request.getSymbolicAmount() != null ? BigDecimal.valueOf(request.getSymbolicAmount()) : null)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(toApiResponse(signatureService.createSignatureSession(internalReq, baseUrl)));
    }

    @Override
    @GetMapping({"/contracts/{contractId}/signatures", "/api/contracts/{contractId}/signatures"})
    public ResponseEntity<List<SignatureSessionResponse>> listSignaturesByContract(@PathVariable UUID contractId) {
        return ResponseEntity.ok(signatureService.listSignaturesByContract(contractId).stream()
                .map(this::toApiResponse)
                .collect(Collectors.toList()));
    }

    @Override
    @GetMapping({"/public/signatures/{token}", "/api/public/signatures/{token}"})
    public ResponseEntity<SignaturePublicViewResponse> getPublicSignatureView(
            @PathVariable String token,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon
    ) {
        String clientIp = servletRequest.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isBlank()) {
            clientIp = servletRequest.getRemoteAddr();
        }
        String userAgent = servletRequest.getHeader("User-Agent");

        BigDecimal latVal = lat != null ? BigDecimal.valueOf(lat) : null;
        BigDecimal lonVal = lon != null ? BigDecimal.valueOf(lon) : null;

        return ResponseEntity.ok(toApiResponse(signatureService.getPublicSignatureView(token, clientIp, userAgent, latVal, lonVal)));
    }

    @Override
    @GetMapping({"/public/signatures/{token}/status", "/api/public/signatures/{token}/status"})
    public ResponseEntity<SignatureSessionResponse> getSignatureStatus(@PathVariable String token) {
        return ResponseEntity.ok(toApiResponse(signatureService.getSignatureStatus(token)));
    }

    @Override
    @PostMapping({"/public/signatures/{token}/fallback", "/api/public/signatures/{token}/fallback"})
    public ResponseEntity<SignatureSessionResponse> selectFallbackMethod(
            @PathVariable String token,
            @RequestBody FallbackSelectionRequest request
    ) {
        br.dev.xb.isperp.signature.FallbackMethod method = request.getFallbackMethod() != null
                ? br.dev.xb.isperp.signature.FallbackMethod.valueOf(request.getFallbackMethod().name())
                : null;
        return ResponseEntity.ok(toApiResponse(signatureService.selectFallbackMethod(token, method, request.getJustification())));
    }

    @Override
    @GetMapping({"/public/signatures/{token}/pdf", "/api/public/signatures/{token}/pdf"})
    public ResponseEntity<Resource> getSignedPdf(@PathVariable String token) {
        br.dev.xb.isperp.dto.SignaturePublicViewResponse view = signatureService.getPublicSignatureView(token, null, null, null, null);
        String content = view.getRenderedContent() != null ? view.getRenderedContent() : "CONTRATO ASSINADO";
        byte[] pdfBytes = content.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header("Content-Type", "text/markdown; charset=UTF-8")
                .header("Content-Disposition", "attachment; filename=\"contrato-" + token + ".md\"")
                .body(new ByteArrayResource(pdfBytes));
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

    private SignaturePublicViewResponse toApiResponse(br.dev.xb.isperp.dto.SignaturePublicViewResponse internal) {
        if (internal == null) return null;
        SignaturePublicViewResponse res = new SignaturePublicViewResponse();
        res.setToken(internal.getToken());
        res.setContractName(internal.getContractName());
        res.setCustomerName(internal.getCustomerName());
        res.setCustomerDocumentMasked(internal.getCustomerDocumentMasked());
        res.setCompanyName(internal.getCompanyName());
        res.setRenderedContent(internal.getRenderedContent());
        res.setConsentClause(internal.getConsentClause());
        if (internal.getStatus() != null) {
            res.setStatus(SignatureStatus.fromValue(internal.getStatus().name()));
        }
        if (internal.getSymbolicAmount() != null) {
            res.setSymbolicAmount(internal.getSymbolicAmount().doubleValue());
        }
        res.setPixCopyPaste(internal.getPixCopyPaste());
        res.setPixQrCodeBase64(internal.getPixQrCodeBase64());
        res.setPayerName(internal.getPayerName());
        res.setPayerBankName(internal.getPayerBankName());
        res.setRejectionReason(internal.getRejectionReason());
        res.setSignedPdfUrl(internal.getSignedPdfUrl());
        res.setDocumentSha256Hash(internal.getDocumentSha256Hash());
        if (internal.getFallbackMethod() != null) {
            res.setFallbackMethod(FallbackMethod.fromValue(internal.getFallbackMethod().name()));
        }
        if (internal.getOnboardingCreditAmount() != null) {
            res.setOnboardingCreditAmount(internal.getOnboardingCreditAmount().doubleValue());
        }
        res.setForensicCertificatePdfUrl(internal.getForensicCertificatePdfUrl());
        res.setExpiresAt(internal.getExpiresAt());
        res.setSignedAt(internal.getSignedAt());
        return res;
    }
}
