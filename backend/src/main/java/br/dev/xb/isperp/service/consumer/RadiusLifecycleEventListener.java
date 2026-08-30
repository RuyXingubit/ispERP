package br.dev.xb.isperp.service.consumer;

import br.dev.xb.isperp.dto.RadiusPolicyConfigResponse;
import br.dev.xb.isperp.event.DomainEvent;
import br.dev.xb.isperp.radius.RadiusLifecycleActionType;
import br.dev.xb.isperp.service.IdempotencyService;
import br.dev.xb.isperp.service.RadiusLifecycleService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class RadiusLifecycleEventListener {

    private static final String CONSUMER_NAME = "RadiusLifecycleEventConsumer";

    private final RadiusLifecycleService radiusLifecycleService;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    @Async("eventTaskExecutor")
    @EventListener
    public void handleDomainEvents(DomainEvent event) {
        String eventType = event.getEventType();

        if ("INVOICE_PAID".equals(eventType)) {
            handleInvoicePaid(event);
        } else if ("CONTRACT_ACCESS_RESTORE_REQUESTED".equals(eventType)) {
            handleTrustUnblockRequested(event);
        } else if ("ONU_PROVISIONED".equals(eventType)) {
            handleOnuProvisioned(event);
        }
    }

    private void handleInvoicePaid(DomainEvent event) {
        log.info("Processando evento INVOICE_PAID para desbloqueio automático no RADIUS: eventId={}", event.getEventId());

        idempotencyService.executeIdempotent(event.getEventId(), CONSUMER_NAME + "_INVOICE_PAID", () -> {
            try {
                Map<String, Object> data = extractPayload(event.getPayload());
                if (data.get("contractId") == null || data.get("customerId") == null) {
                    return;
                }

                UUID contractId = UUID.fromString(data.get("contractId").toString());
                UUID customerId = UUID.fromString(data.get("customerId").toString());
                String paymentMethod = data.get("paymentMethod") != null ? data.get("paymentMethod").toString() : "PIX";

                RadiusPolicyConfigResponse config = radiusLifecycleService.getPolicyConfigResponse();
                if (!config.isUnblockOnPayment()) {
                    log.info("Desbloqueio por pagamento está desativado nas políticas.");
                    return;
                }

                // Se o cliente regularizou todas as pendências vencidas
                if (radiusLifecycleService.isCustomerEligibleForUnblock(customerId)) {
                    radiusLifecycleService.executeInstantUnblock(
                            contractId,
                            "Pagamento compensado com sucesso (" + paymentMethod + ")",
                            RadiusLifecycleActionType.PAYMENT_UNBLOCK
                    );
                } else {
                    log.info("Cliente {} ainda possui outras faturas vencidas em aberto. Mantendo bloqueio.", customerId);
                }
            } catch (Exception e) {
                log.error("Erro ao processar desbloqueio no RADIUS para evento {}: {}", event.getEventId(), e.getMessage());
            }
        });
    }

    private void handleTrustUnblockRequested(DomainEvent event) {
        log.info("Processando evento CONTRACT_ACCESS_RESTORE_REQUESTED para liberação em confiança no RADIUS: eventId={}", event.getEventId());

        idempotencyService.executeIdempotent(event.getEventId(), CONSUMER_NAME + "_TRUST_UNBLOCK", () -> {
            try {
                Map<String, Object> data = extractPayload(event.getPayload());
                if (data.get("contractId") == null) {
                    return;
                }

                UUID contractId = UUID.fromString(data.get("contractId").toString());
                String unblockType = data.get("unblockType") != null ? data.get("unblockType").toString() : "MANUAL";

                radiusLifecycleService.executeInstantUnblock(
                        contractId,
                        "Desbloqueio em Confiança (" + unblockType + ")",
                        RadiusLifecycleActionType.TRUST_UNBLOCK
                );
            } catch (Exception e) {
                log.error("Erro ao processar desbloqueio em confiança no RADIUS: {}", e.getMessage());
            }
        });
    }

    private void handleOnuProvisioned(DomainEvent event) {
        log.info("Processando evento ONU_PROVISIONED para sincronização de credenciais no RADIUS: eventId={}", event.getEventId());

        idempotencyService.executeIdempotent(event.getEventId(), CONSUMER_NAME + "_ONU_PROVISIONED", () -> {
            try {
                Map<String, Object> data = extractPayload(event.getPayload());
                if (data.get("contractId") == null) {
                    return;
                }

                UUID contractId = UUID.fromString(data.get("contractId").toString());
                radiusLifecycleService.syncContractToRadius(contractId);
            } catch (Exception e) {
                log.error("Erro ao sincronizar credenciais no RADIUS após provisionamento: {}", e.getMessage());
            }
        });
    }

    private Map<String, Object> extractPayload(Object payload) {
        if (payload instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) payload;
            return map;
        } else if (payload instanceof String str) {
            try {
                return objectMapper.readValue(str, new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                return Map.of();
            }
        }
        return Map.of();
    }
}
