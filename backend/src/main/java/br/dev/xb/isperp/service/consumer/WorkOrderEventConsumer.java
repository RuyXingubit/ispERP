package br.dev.xb.isperp.service.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import br.dev.xb.isperp.entity.Contract;
import br.dev.xb.isperp.event.DomainEvent;
import br.dev.xb.isperp.service.ContractService;
import br.dev.xb.isperp.service.IdempotencyService;
import br.dev.xb.isperp.service.WorkOrderService;
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
public class WorkOrderEventConsumer {

    private static final String CONSUMER_NAME_CONTRACT = "WorkOrderCreationConsumer";
    private static final String CONSUMER_NAME_COMPLETION = "ContractActivationConsumer";

    private final WorkOrderService workOrderService;
    private final ContractService contractService;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    @Async("eventTaskExecutor")
    @EventListener
    public void handleDomainEvent(DomainEvent event) {
        if ("CONTRACT_CREATED".equals(event.getEventType())) {
            handleContractCreated(event);
        } else if ("WORK_ORDER_COMPLETED".equals(event.getEventType())) {
            handleWorkOrderCompleted(event);
        }
    }

    private void handleContractCreated(DomainEvent event) {
        log.info("Criando O.S. pendente a partir do evento CONTRACT_CREATED: eventId={}", event.getEventId());

        idempotencyService.executeIdempotent(event.getEventId(), CONSUMER_NAME_CONTRACT, () -> {
            try {
                Map<String, Object> data = extractPayload(event.getPayload());

                UUID contractId = UUID.fromString(data.get("contractId").toString());
                UUID customerId = UUID.fromString(data.get("customerId").toString());
                String notes = "Endereço: " + data.get("installationAddress") + " - " + data.get("city") + "/" + data.get("state");

                workOrderService.createInitialInstallationWorkOrder(contractId, customerId, notes);
                log.info("Ordem de Serviço criada com status PENDING_SCHEDULE para o contrato {}", contractId);
            } catch (Exception e) {
                log.error("Erro ao criar Ordem de Serviço para o contrato: {}", e.getMessage(), e);
                throw new RuntimeException("Falha ao criar O.S. de instalação", e);
            }
        });
    }

    private void handleWorkOrderCompleted(DomainEvent event) {
        log.info("Ativando contrato a partir do evento WORK_ORDER_COMPLETED: eventId={}", event.getEventId());

        idempotencyService.executeIdempotent(event.getEventId(), CONSUMER_NAME_COMPLETION, () -> {
            try {
                Map<String, Object> data = extractPayload(event.getPayload());
                UUID contractId = UUID.fromString(data.get("contractId").toString());

                contractService.updateStatus(contractId, Contract.ContractStatus.ACTIVE);
                log.info("Contrato {} ativado com sucesso após conclusão da instalação técnica", contractId);
            } catch (Exception e) {
                log.error("Erro ao ativar contrato após O.S. concluída: {}", e.getMessage(), e);
                throw new RuntimeException("Falha ao ativar contrato após conclusão técnica", e);
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
