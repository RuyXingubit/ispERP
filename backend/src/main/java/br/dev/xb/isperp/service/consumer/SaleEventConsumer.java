package br.dev.xb.isperp.service.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import br.dev.xb.isperp.entity.Contract;
import br.dev.xb.isperp.entity.Customer;
import br.dev.xb.isperp.event.DomainEvent;
import br.dev.xb.isperp.event.GenericDomainEvent;
import br.dev.xb.isperp.repository.ContractRepository;
import br.dev.xb.isperp.repository.CustomerRepository;
import br.dev.xb.isperp.service.DomainEventPublisher;
import br.dev.xb.isperp.service.IdempotencyService;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
                Map<String, Object> data = extractPayload(event.getPayload());

                String customerCpf = (String) data.get("customerCpf");
                String customerName = (String) data.get("customerName");
                String customerEmail = (String) data.get("customerEmail");
                String customerPhone = (String) data.get("customerPhone");
                String installationAddress = (String) data.get("installationAddress");
                String city = (String) data.get("city");
                String state = (String) data.get("state");
                String zipCode = (String) data.get("zipCode");
                UUID planId = UUID.fromString(data.get("planId").toString());
                UUID saleId = UUID.fromString(data.get("saleId").toString());
                BigDecimal monthlyFee = new BigDecimal(data.get("monthlyFee").toString());
                Integer preferredDueDate = data.get("preferredDueDate") != null 
                        ? Integer.parseInt(data.get("preferredDueDate").toString()) : 10;

                // 1. Cadastrar ou atualizar cliente
                Customer customer = customerRepository.findByCpf(customerCpf).orElseGet(() -> {
                    log.info("Cadastrando novo cliente a partir da venda: CPF={}", customerCpf);
                    return Customer.builder()
                            .id(UuidCreatorUtils.generateUuidV7())
                            .cpf(customerCpf)
                            .active(true)
                            .build();
                });

                customer.setName(customerName);
                customer.setEmail(customerEmail);
                customer.setPhone(customerPhone);
                customer.setAddress(installationAddress);
                customer.setCity(city);
                customer.setState(state);
                customer.setZipCode(zipCode);
                customer.setActive(true);

                Customer savedCustomer = customerRepository.save(customer);

                // 2. Criar contrato em PENDING_INSTALLATION
                String contractNumber = "CTR-" + System.currentTimeMillis();
                Contract contract = Contract.builder()
                        .id(UuidCreatorUtils.generateUuidV7())
                        .customerId(savedCustomer.getId())
                        .planId(planId)
                        .saleId(saleId)
                        .contractNumber(contractNumber)
                        .status(Contract.ContractStatus.PENDING_INSTALLATION)
                        .monthlyFee(monthlyFee)
                        .dueDay(preferredDueDate)
                        .installationAddress(installationAddress)
                        .city(city)
                        .state(state)
                        .zipCode(zipCode)
                        .build();

                Contract savedContract = contractRepository.save(contract);
                log.info("Contrato {} gerado com sucesso para o cliente {}", savedContract.getContractNumber(), savedCustomer.getName());

                // 3. Emitir ContractCreatedEvent para o próximo passo do fluxo (Ordem de Serviço na Milestone 4)
                Map<String, Object> contractEventData = new HashMap<>();
                contractEventData.put("contractId", savedContract.getId().toString());
                contractEventData.put("contractNumber", savedContract.getContractNumber());
                contractEventData.put("customerId", savedCustomer.getId().toString());
                contractEventData.put("customerName", savedCustomer.getName());
                contractEventData.put("customerPhone", savedCustomer.getPhone());
                contractEventData.put("planId", planId.toString());
                contractEventData.put("installationAddress", installationAddress);
                contractEventData.put("city", city);
                contractEventData.put("state", state);
                contractEventData.put("zipCode", zipCode);

                GenericDomainEvent contractCreatedEvent = GenericDomainEvent.builder()
                        .eventId(UuidCreatorUtils.generateUuidV7())
                        .eventType("CONTRACT_CREATED")
                        .aggregateType("Contract")
                        .aggregateId(savedContract.getId().toString())
                        .payload(contractEventData)
                        .build();

                domainEventPublisher.publish(contractCreatedEvent);

            } catch (Exception e) {
                log.error("Erro ao processar evento SALE_SUBMITTED {}: {}", event.getEventId(), e.getMessage(), e);
                throw new RuntimeException("Falha ao processar venda para cliente e contrato", e);
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
            throw new RuntimeException("Erro ao converter payload para Map", e);
        }
    }
}
