package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.contract.PaymentWebhooksApi;
import br.dev.xb.isperp.api.dto.PaymentWebhookResponse;
import br.dev.xb.isperp.service.PaymentWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping({"/webhooks/payments", "/api/webhooks/payments"})
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class PaymentWebhookController implements PaymentWebhooksApi {

    private final PaymentWebhookService paymentWebhookService;

    @Override
    @PostMapping("/{gatewayType}")
    public ResponseEntity<PaymentWebhookResponse> handlePaymentWebhook(
            @PathVariable String gatewayType,
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature) {
        log.info("Recebida requisição de webhook para {}: payload={}", gatewayType, payload);

        try {
            paymentWebhookService.processPaymentWebhook(gatewayType, payload, signature);
            PaymentWebhookResponse response = new PaymentWebhookResponse();
            response.setReceived(true);
            response.setStatus("PROCESSED");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erro ao processar webhook {}: {}", gatewayType, e.getMessage());
            PaymentWebhookResponse response = new PaymentWebhookResponse();
            response.setReceived(false);
            response.setError(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
