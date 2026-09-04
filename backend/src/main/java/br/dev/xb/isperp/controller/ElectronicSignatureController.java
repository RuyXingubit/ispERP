package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.dto.*;
import br.dev.xb.isperp.service.ElectronicSignatureService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ElectronicSignatureController {

    private final ElectronicSignatureService signatureService;

    // --- Endpoints Administrativos (Internos) ---

    @PostMapping("/api/contracts/signatures")
    public ResponseEntity<SignatureSessionResponse> createSignatureSession(
            @Valid @RequestBody CreateSignatureSessionRequest request,
            HttpServletRequest servletRequest
    ) {
        String baseUrl = servletRequest.getScheme() + "://" + servletRequest.getServerName() +
                (servletRequest.getServerPort() != 80 && servletRequest.getServerPort() != 443 ? ":" + servletRequest.getServerPort() : "");
        return ResponseEntity.status(HttpStatus.CREATED).body(signatureService.createSignatureSession(request, baseUrl));
    }

    @GetMapping("/api/contracts/{contractId}/signatures")
    public ResponseEntity<List<SignatureSessionResponse>> getSignaturesByContract(@PathVariable UUID contractId) {
        return ResponseEntity.ok(signatureService.listSignaturesByContract(contractId));
    }

    // --- Endpoints Públicos (Assinante / Página de Assinatura) ---

    @GetMapping("/api/public/signatures/{token}")
    public ResponseEntity<SignaturePublicViewResponse> getPublicSignatureView(
            @PathVariable String token,
            @RequestParam(required = false) BigDecimal lat,
            @RequestParam(required = false) BigDecimal lon,
            HttpServletRequest servletRequest
    ) {
        String clientIp = servletRequest.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isBlank()) {
            clientIp = servletRequest.getRemoteAddr();
        }
        String userAgent = servletRequest.getHeader("User-Agent");

        return ResponseEntity.ok(signatureService.getPublicSignatureView(token, clientIp, userAgent, lat, lon));
    }

    @GetMapping("/api/public/signatures/{token}/status")
    public ResponseEntity<SignatureSessionResponse> getSignatureStatus(@PathVariable String token) {
        return ResponseEntity.ok(signatureService.getSignatureStatus(token));
    }

    @PostMapping("/api/public/signatures/{token}/fallback")
    public ResponseEntity<SignatureSessionResponse> selectFallbackMethod(
            @PathVariable String token,
            @Valid @RequestBody FallbackSelectionRequest request
    ) {
        return ResponseEntity.ok(signatureService.selectFallbackMethod(token, request.getFallbackMethod(), request.getJustification()));
    }

    @GetMapping("/api/public/signatures/{token}/pdf")
    public ResponseEntity<byte[]> getSignedPdf(@PathVariable String token) {
        SignaturePublicViewResponse view = signatureService.getPublicSignatureView(token, null, null, null, null);
        String content = view.getRenderedContent() != null ? view.getRenderedContent() : "CONTRATO ASSINADO";
        byte[] pdfBytes = content.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header("Content-Type", "text/markdown; charset=UTF-8")
                .header("Content-Disposition", "attachment; filename=\"contrato-" + token + ".md\"")
                .body(pdfBytes);
    }
}
