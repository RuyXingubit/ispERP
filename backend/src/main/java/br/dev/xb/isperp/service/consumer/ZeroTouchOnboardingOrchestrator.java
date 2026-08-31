package br.dev.xb.isperp.service.consumer;

import br.dev.xb.isperp.dto.CreateSignatureSessionRequest;
import br.dev.xb.isperp.entity.*;
import br.dev.xb.isperp.event.DomainEvent;
import br.dev.xb.isperp.event.GenericDomainEvent;
import br.dev.xb.isperp.repository.*;
import br.dev.xb.isperp.service.*;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class ZeroTouchOnboardingOrchestrator {

    private static final String CONSUMER_NAME = "ZeroTouchOnboardingOrchestrator";

    private final SaleRepository saleRepository;
    private final CustomerRepository customerRepository;
    private final ContractRepository contractRepository;
    private final WorkOrderRepository workOrderRepository;
    private final ElectronicSignatureService signatureService;
    private final InstallationDemandService demandService;
    private final DomainEventPublisher domainEventPublisher;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    @Async("eventTaskExecutor")
    @EventListener
    public void handleDomainEvent(DomainEvent event) {
        String eventType = event.getEventType();

        if ("SALE_SUBMITTED".equals(eventType)) {
            handleSaleSubmitted(event);
        } else if ("CONTRACT_SIGNED".equals(eventType)) {
            handleContractSigned(event);
        } else if ("WORK_ORDER_COMPLETED".equals(eventType)) {
            handleWorkOrderCompleted(event);
        }
    }

    private void handleSaleSubmitted(DomainEvent event) {
        log.info("Zero-Touch: Processando SALE_SUBMITTED eventId={}", event.getEventId());

        idempotencyService.executeIdempotent(event.getEventId(), CONSUMER_NAME + "-SaleSubmitted", () -> {
            try {
                Map<String, Object> data = extractPayload(event.getPayload());

                UUID saleId = UUID.fromString(data.get("saleId").toString());
                UUID planId = UUID.fromString(data.get("planId").toString());
                String customerCpf = (String) data.get("customerCpf");
                String customerName = (String) data.get("customerName");
                String customerEmail = (String) data.get("customerEmail");
                String customerPhone = (String) data.get("customerPhone");
                String installationAddress = (String) data.get("installationAddress");
                String city = (String) data.get("city");
                String state = (String) data.get("state");
                String zipCode = (String) data.get("zipCode");
                BigDecimal monthlyFee = new BigDecimal(data.get("monthlyFee").toString());
                Integer preferredDueDate = data.get("preferredDueDate") != null
                        ? Integer.parseInt(data.get("preferredDueDate").toString()) : 10;

                // 1. Atualiza status da venda para PENDING_PAYMENT_SIGNATURE
                saleRepository.findById(saleId).ifPresent(sale -> {
                    sale.setStatus(Sale.SaleStatus.PENDING_PAYMENT_SIGNATURE);
                    saleRepository.save(sale);
                });

                // 2. Cadastra ou recupera cliente
                Customer customer = customerRepository.findByCpf(customerCpf).orElseGet(() -> {
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
                Customer savedCustomer = customerRepository.save(customer);

                // 3. Cria contrato em status PENDING_SIGNATURE (ou PENDING_INSTALLATION se já assinado)
                String contractNumber = "CTR-" + System.currentTimeMillis();
                Contract contract = Contract.builder()
                        .id(UuidCreatorUtils.generateUuidV7())
                        .customerId(savedCustomer.getId())
                        .planId(planId)
                        .saleId(saleId)
                        .contractNumber(contractNumber)
                        .status(Contract.ContractStatus.DRAFT)
                        .monthlyFee(monthlyFee)
                        .dueDay(preferredDueDate)
                        .installationAddress(installationAddress)
                        .city(city)
                        .state(state)
                        .zipCode(zipCode)
                        .build();

                Contract savedContract = contractRepository.save(contract);
                log.info("Contrato {} gerado em DRAFT para assinatura Pix do cliente {}", contractNumber, customerName);

                // 4. Cria sessão de assinatura Pix de R$ 1,00
                CreateSignatureSessionRequest sigReq = CreateSignatureSessionRequest.builder()
                        .contractId(savedContract.getId())
                        .symbolicAmount(BigDecimal.ONE)
                        .build();

                signatureService.createSignatureSession(sigReq, null);

            } catch (Exception e) {
                log.error("Erro no Zero-Touch ao processar SALE_SUBMITTED: {}", e.getMessage(), e);
                throw new RuntimeException("Falha no Zero-Touch SaleSubmitted", e);
            }
        });
    }

    private void handleContractSigned(DomainEvent event) {
        log.info("Zero-Touch: Processando CONTRACT_SIGNED eventId={}", event.getEventId());

        idempotencyService.executeIdempotent(event.getEventId(), CONSUMER_NAME + "-ContractSigned", () -> {
            try {
                Map<String, Object> data = extractPayload(event.getPayload());
                UUID contractId = UUID.fromString(data.get("contractId").toString());

                Contract contract = contractRepository.findById(contractId)
                        .orElseThrow(() -> new NoSuchElementException("Contrato não encontrado: " + contractId));

                // 1. Atualiza status do contrato para PENDING_INSTALLATION
                contract.setStatus(Contract.ContractStatus.PENDING_INSTALLATION);
                contractRepository.save(contract);

                // 2. Se houver venda vinculada, marca como PROCESSED
                if (contract.getSaleId() != null) {
                    saleRepository.findById(contract.getSaleId()).ifPresent(sale -> {
                        sale.setStatus(Sale.SaleStatus.PROCESSED);
                        saleRepository.save(sale);
                    });
                }

                // 3. Cria Ordem de Serviço de Instalação se não existir
                WorkOrder workOrder = workOrderRepository.findByContractId(contractId).stream().findFirst().orElseGet(() -> {
                    WorkOrder newWo = WorkOrder.builder()
                            .id(UuidCreatorUtils.generateUuidV7())
                            .contractId(contractId)
                            .customerId(contract.getCustomerId())
                            .type(WorkOrder.WorkOrderType.INSTALACAO)
                            .status(WorkOrder.WorkOrderStatus.PENDING_SCHEDULE)
                            .notes("O.S. de Instalação gerada automaticamente pelo Zero-Touch Onboarding após confirmação Pix R$ 1,00.")
                            .build();
                    return workOrderRepository.save(newWo);
                });

                // 4. Dimensiona materiais FTTH (Drop e Kit)
                InstallationMaterialDemand demand = demandService.generateDemandForWorkOrder(workOrder.getId());

                // 5. Emite evento INSTALLATION_DEMAND_GENERATED para painel de despacho do Suporte
                Map<String, Object> demandPayload = new HashMap<>();
                demandPayload.put("workOrderId", workOrder.getId().toString());
                demandPayload.put("contractId", contract.getId().toString());
                demandPayload.put("customerId", contract.getCustomerId().toString());
                demandPayload.put("demandId", demand.getId().toString());
                demandPayload.put("estimatedDropMeters", demand.getEstimatedDropMeters());
                demandPayload.put("onuModelRequired", demand.getOnuModelRequired());

                GenericDomainEvent demandEvent = GenericDomainEvent.builder()
                        .eventId(UuidCreatorUtils.generateUuidV7())
                        .eventType("INSTALLATION_DEMAND_GENERATED")
                        .aggregateType("InstallationMaterialDemand")
                        .aggregateId(demand.getId().toString())
                        .payload(demandPayload)
                        .build();

                domainEventPublisher.publish(demandEvent);
                log.info("Zero-Touch: O.S. {} e Demanda de Materiais {} geradas com sucesso para o contrato {}",
                        workOrder.getId(), demand.getId(), contract.getContractNumber());

            } catch (Exception e) {
                log.error("Erro no Zero-Touch ao processar CONTRACT_SIGNED: {}", e.getMessage(), e);
                throw new RuntimeException("Falha no Zero-Touch ContractSigned", e);
            }
        });
    }

    private void handleWorkOrderCompleted(DomainEvent event) {
        log.info("Zero-Touch: Processando WORK_ORDER_COMPLETED eventId={}", event.getEventId());

        idempotencyService.executeIdempotent(event.getEventId(), CONSUMER_NAME + "-WorkOrderCompleted", () -> {
            try {
                Map<String, Object> data = extractPayload(event.getPayload());
                UUID contractId = UUID.fromString(data.get("contractId").toString());

                contractRepository.findById(contractId).ifPresent(c -> {
                    c.setStatus(Contract.ContractStatus.ACTIVE);
                    contractRepository.save(c);
                    log.info("Zero-Touch: Contrato {} ativado definitivamente após conclusão da O.S.!", c.getContractNumber());
                });

            } catch (Exception e) {
                log.error("Erro no Zero-Touch ao processar WORK_ORDER_COMPLETED: {}", e.getMessage(), e);
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
