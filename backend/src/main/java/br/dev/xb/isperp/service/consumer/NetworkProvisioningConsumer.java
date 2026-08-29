package br.dev.xb.isperp.service.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import br.dev.xb.isperp.entity.Contract;
import br.dev.xb.isperp.event.DomainEvent;
import br.dev.xb.isperp.service.ContractService;
import br.dev.xb.isperp.service.IdempotencyService;
import br.dev.xb.isperp.service.NetworkProvisioningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class NetworkProvisioningConsumer {

    private static final String CONSUMER_NAME = "NetworkProvisioningConsumer";

    private final NetworkProvisioningService provisioningService;
    private final ContractService contractService;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    @Async("eventTaskExecutor")
    @EventListener
    public void handleDomainEvent(DomainEvent event) {
        String type = event.getEventType();

        if ("WORK_ORDER_COMPLETED".equals(type)) {
            handleWorkOrderCompleted(event);
        } else if ("INVOICE_PAID".equals(type) || "INTERNET_ACCESS_UNBLOCKED".equals(type)) {
            handleInvoicePaid(event);
        } else if ("PLAN_UPGRADED".equals(type)) {
            handlePlanUpgraded(event);
        }
    }

    private void handlePlanUpgraded(DomainEvent event) {
        idempotencyService.executeIdempotent(event.getEventId(), CONSUMER_NAME + "_PlanUpgradeOLT", () -> {
            Map<String, Object> data = extractPayload(event.getPayload());
            String contractIdStr = (String) data.get("contractId");
            if (contractIdStr != null) {
                UUID contractId = UUID.fromString(contractIdStr);
                log.info("Reprovisionando velocidade na OLT para o contrato {} após upgrade", contractId);
                try {
                    Contract contract = contractService.getContractById(contractId).orElse(null);
                    if (contract != null) {
                        provisioningService.unblockInternetAccess(contractId);
                    }
                } catch (Exception e) {
                    log.debug("Erro ao reprovisionar OLT após upgrade: {}", e.getMessage());
                }
            }
        });
    }

    private void handleWorkOrderCompleted(DomainEvent event) {
        idempotencyService.executeIdempotent(event.getEventId(), CONSUMER_NAME + "_WOComplete", () -> {
            Map<String, Object> data = extractPayload(event.getPayload());
            String contractIdStr = (String) data.get("contractId");
            String onuMac = (String) data.get("onuMac");
            String onuSerial = (String) data.get("onuSerial");
            Object signalObj = data.get("opticalSignalDbm");

            if (contractIdStr != null && onuMac != null) {
                UUID contractId = UUID.fromString(contractIdStr);
                Contract contract = contractService.getContractById(contractId)
                        .orElseThrow(() -> new RuntimeException("Contrato não encontrado"));

                BigDecimal signalDbm = signalObj != null ? new BigDecimal(signalObj.toString()) : new BigDecimal("-19.50");
                provisioningService.provisionOnuForContract(contract, onuMac, onuSerial != null ? onuSerial : onuMac, signalDbm);
            }
        });
    }

    private void handleInvoicePaid(DomainEvent event) {
        idempotencyService.executeIdempotent(event.getEventId(), CONSUMER_NAME + "_InvoicePaidUnblock", () -> {
            Map<String, Object> data = extractPayload(event.getPayload());
            String contractIdStr = (String) data.get("contractId");

            if (contractIdStr != null) {
                UUID contractId = UUID.fromString(contractIdStr);
                try {
                    provisioningService.unblockInternetAccess(contractId);
                } catch (Exception e) {
                    log.debug("Nenhuma ONU bloqueada encontrada para desbloqueio do contrato {}", contractId);
                }
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
