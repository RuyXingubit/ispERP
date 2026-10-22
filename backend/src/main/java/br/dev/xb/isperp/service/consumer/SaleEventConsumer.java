package br.dev.xb.isperp.service.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import br.dev.xb.isperp.entity.Contract;
import br.dev.xb.isperp.entity.Customer;
import br.dev.xb.isperp.event.DomainEvent;
import br.dev.xb.isperp.event.GenericDomainEvent;
import br.dev.xb.isperp.repository.ContractRepository;
import br.dev.xb.isperp.repository.CustomerRepository;
import br.dev.xb.isperp.service.DomainEventPublisher;
import br.dev.xb.isperp.service.IdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class SaleEventConsumer {

    private static final String CONSUMER_NAME = "SaleToCustomerAndContractConsumer";

    private final CustomerRepository customerRepository;
    private final ContractRepository contractRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    @Async("eventTaskExecutor")
    @EventListener
    public void handleSaleSubmittedEvent(DomainEvent event) {
        if (!"SALE_SUBMITTED".equals(event.getEventType())) {
            return;
        }

        log.info("Consumindo evento SALE_SUBMITTED: eventId={}", event.getEventId());

        idempotencyService.executeIdempotent(event.getEventId(), CONSUMER_NAME, () -> {
            try {
                Map<String, Object> data = ConsumerPayloadUtils.extractPayload(objectMapper, event.getPayload());

                ConsumerPayloadUtils.CustomerAndContract result = ConsumerPayloadUtils.upsertCustomerAndCreateContract(
                        customerRepository,
                        contractRepository,
                        data,
                        Contract.ContractStatus.PENDING_INSTALLATION
                );
                Customer savedCustomer = result.customer();
                Contract savedContract = result.contract();
                log.info("Contrato {} gerado com sucesso para o cliente {}", savedContract.getContractNumber(), savedCustomer.getName());

                // 3. Emitir ContractCreatedEvent para o próximo passo do fluxo (Ordem de Serviço na Milestone 4)
                Map<String, Object> contractEventData = new HashMap<>();
                contractEventData.put("contractId", savedContract.getId().toString());
                contractEventData.put("contractNumber", savedContract.getContractNumber());
                contractEventData.put("customerId", savedCustomer.getId().toString());
                contractEventData.put("customerName", savedCustomer.getName());
                contractEventData.put("customerPhone", savedCustomer.getPhone());
                contractEventData.put("planId", savedContract.getPlanId().toString());
                contractEventData.put("installationAddress", savedContract.getInstallationAddress());
                contractEventData.put("city", savedContract.getCity());
                contractEventData.put("state", savedContract.getState());
                contractEventData.put("zipCode", savedContract.getZipCode());

                GenericDomainEvent contractCreatedEvent = ConsumerPayloadUtils.buildEvent(
                        "CONTRACT_CREATED",
                        "Contract",
                        savedContract.getId().toString(),
                        contractEventData
                );

                domainEventPublisher.publish(contractCreatedEvent);

            } catch (Exception e) {
                log.error("Erro ao processar evento SALE_SUBMITTED {}: {}", event.getEventId(), e.getMessage(), e);
                throw new RuntimeException("Falha ao processar venda para cliente e contrato", e);
            }
        });
    }
}
