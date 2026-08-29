package br.dev.xb.isperp.service.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import br.dev.xb.isperp.event.DomainEvent;
import br.dev.xb.isperp.event.GenericDomainEvent;
import br.dev.xb.isperp.service.DomainEventPublisher;
import br.dev.xb.isperp.service.IdempotencyService;
import br.dev.xb.isperp.service.InventoryService;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryStockConsumer {

    private static final String CONSUMER_NAME = "InventoryStockConsumer";

    private final InventoryService inventoryService;
    private final DomainEventPublisher domainEventPublisher;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    @Async("eventTaskExecutor")
    @EventListener
    public void handleContractCreated(DomainEvent event) {
        if (!"CONTRACT_CREATED".equals(event.getEventType())) {
            return;
        }

        log.info("Verificando e reservando estoque para novo contrato: eventId={}", event.getEventId());

        idempotencyService.executeIdempotent(event.getEventId(), CONSUMER_NAME, () -> {
            try {
                Map<String, Object> data = extractPayload(event.getPayload());
                UUID contractId = UUID.fromString(data.get("contractId").toString());

                List<String> warnings = inventoryService.checkAndReserveInstallationMaterials(contractId);

                Map<String, Object> stockPayload = new HashMap<>();
                stockPayload.put("contractId", contractId.toString());
                stockPayload.put("warnings", warnings);
                stockPayload.put("status", warnings.isEmpty() ? "RESERVED" : "RESERVED_WITH_WARNINGS");

                GenericDomainEvent stockEvent = GenericDomainEvent.builder()
                        .eventId(UuidCreatorUtils.generateUuidV7())
                        .eventType("STOCK_VERIFIED")
                        .aggregateType("Contract")
                        .aggregateId(contractId.toString())
                        .payload(stockPayload)
                        .build();

                domainEventPublisher.publish(stockEvent);
                log.info("Checagem de estoque concluída para contrato {}: status={}", contractId, stockPayload.get("status"));

            } catch (Exception e) {
                log.error("Erro ao verificar insumos no estoque: {}", e.getMessage(), e);
                throw new RuntimeException("Falha ao verificar estoque para contrato", e);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractPayload(Object payload) {
        if (payload instanceof Map) {
            return (Map<String, Object>) payload;
        }
        try {
            return objectMapper.readValue(payload.toString(), new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Erro ao converter payload", e);
        }
    }
}
