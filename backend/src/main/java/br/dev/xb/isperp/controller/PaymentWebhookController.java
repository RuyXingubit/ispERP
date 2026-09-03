package br.dev.xb.isperp.controller;

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
public class PaymentWebhookController {

    private final PaymentWebhookService paymentWebhookService;

    @PostMapping("/{gatewayType}")
    public ResponseEntity<Map<String, Object>> handlePaymentWebhook(
            @PathVariable String gatewayType,
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature) {
        log.info("Recebida requisição de webhook para {}: payload={}", gatewayType, payload);

        try {
            paymentWebhookService.processPaymentWebhook(gatewayType, payload, signature);
            return ResponseEntity.ok(Map.of("received", true, "status", "PROCESSED"));
        } catch (Exception e) {
            log.error("Erro ao processar webhook {}: {}", gatewayType, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("received", false, "error", e.getMessage()));
        }
    }
}
