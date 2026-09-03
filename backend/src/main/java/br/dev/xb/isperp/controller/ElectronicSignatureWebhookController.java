package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.dto.PixSignatureWebhookRequest;
import br.dev.xb.isperp.dto.SignatureSessionResponse;
import br.dev.xb.isperp.service.ElectronicSignatureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/webhooks/signatures", "/api/webhooks/signatures"})
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ElectronicSignatureWebhookController {

    private final ElectronicSignatureService signatureService;

    @PostMapping("/pix")
    public ResponseEntity<SignatureSessionResponse> handlePixSignatureWebhook(@RequestBody PixSignatureWebhookRequest request) {
        log.info("Recebida notificação webhook Pix de assinatura: TxID={}", request.getTxid());
        SignatureSessionResponse response = signatureService.processPixSignatureWebhook(request);
        return ResponseEntity.ok(response);
    }
}
