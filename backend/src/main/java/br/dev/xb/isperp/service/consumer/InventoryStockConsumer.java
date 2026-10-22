package br.dev.xb.isperp.service.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import br.dev.xb.isperp.event.DomainEvent;
import br.dev.xb.isperp.event.GenericDomainEvent;
import br.dev.xb.isperp.service.DomainEventPublisher;
import br.dev.xb.isperp.service.IdempotencyService;
import br.dev.xb.isperp.service.InventoryService;
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
@SuppressWarnings("null")
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
                Map<String, Object> data = ConsumerPayloadUtils.extractPayload(objectMapper, event.getPayload());
                UUID contractId = UUID.fromString(data.get("contractId").toString());

                List<String> warnings = inventoryService.checkAndReserveInstallationMaterials(contractId);

                Map<String, Object> stockPayload = new HashMap<>();
                stockPayload.put("contractId", contractId.toString());
                stockPayload.put("warnings", warnings);
                stockPayload.put("status", warnings.isEmpty() ? "RESERVED" : "RESERVED_WITH_WARNINGS");

                GenericDomainEvent stockEvent = ConsumerPayloadUtils.buildEvent(
                        "STOCK_VERIFIED",
                        "Contract",
                        contractId.toString(),
                        stockPayload
                );

                domainEventPublisher.publish(stockEvent);
                log.info("Checagem de estoque concluída para contrato {}: status={}", contractId, stockPayload.get("status"));

            } catch (Exception e) {
                log.error("Erro ao verificar insumos no estoque: {}", e.getMessage(), e);
                throw new RuntimeException("Falha ao verificar estoque para contrato", e);
            }
        });
    }
}
