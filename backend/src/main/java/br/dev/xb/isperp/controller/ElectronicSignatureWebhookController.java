package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.contract.SignatureWebhooksApi;
import br.dev.xb.isperp.api.dto.PixSignatureWebhookRequest;
import br.dev.xb.isperp.api.dto.SignatureSessionResponse;
import br.dev.xb.isperp.service.ElectronicSignatureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequiredArgsConstructor
@Slf4j
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
        return ResponseEntity.ok(ElectronicSignatureController.toApiResponse(internalRes));
    }
}
