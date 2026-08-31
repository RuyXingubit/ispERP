package br.dev.xb.isperp.service.consumer;

import br.dev.xb.isperp.entity.*;
import br.dev.xb.isperp.event.DomainEvent;
import br.dev.xb.isperp.event.GenericDomainEvent;
import br.dev.xb.isperp.repository.*;
import br.dev.xb.isperp.service.DomainEventPublisher;
import br.dev.xb.isperp.service.ElectronicSignatureService;
import br.dev.xb.isperp.service.IdempotencyService;
import br.dev.xb.isperp.service.InstallationDemandService;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ZeroTouchOnboardingOrchestratorTest {

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private WorkOrderRepository workOrderRepository;

    @Mock
    private ElectronicSignatureService signatureService;

    @Mock
    private InstallationDemandService demandService;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @Mock
    private IdempotencyService idempotencyService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ZeroTouchOnboardingOrchestrator orchestrator;

    private UUID saleId;
    private UUID contractId;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        orchestrator = new ZeroTouchOnboardingOrchestrator(
                saleRepository,
                customerRepository,
                contractRepository,
                workOrderRepository,
                signatureService,
                demandService,
                domainEventPublisher,
                idempotencyService,
                objectMapper
        );

        saleId = UuidCreatorUtils.generateUuidV7();
        contractId = UuidCreatorUtils.generateUuidV7();
        customerId = UuidCreatorUtils.generateUuidV7();

        // Faz com que idempotencyService execute o Runnable
        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(2);
            action.run();
            return null;
        }).when(idempotencyService).executeIdempotent(any(), any(), any());
    }

    @Test
    @DisplayName("Deve processar SALE_SUBMITTED e criar contrato em DRAFT com sessão de assinatura Pix")
    void testHandleSaleSubmitted() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("saleId", saleId.toString());
        payload.put("planId", UuidCreatorUtils.generateUuidV7().toString());
        payload.put("customerCpf", "12345678900");
        payload.put("customerName", "Maria Souza");
        payload.put("customerPhone", "91988887777");
        payload.put("installationAddress", "Rua das Flores, 100");
        payload.put("city", "Belém");
        payload.put("state", "PA");
        payload.put("zipCode", "66000000");
        payload.put("monthlyFee", "99.90");

        DomainEvent event = GenericDomainEvent.builder()
                .eventId(UuidCreatorUtils.generateUuidV7())
                .eventType("SALE_SUBMITTED")
                .payload(payload)
                .build();

        when(saleRepository.findById(saleId)).thenReturn(Optional.of(Sale.builder().id(saleId).build()));
        when(customerRepository.findByCpf("12345678900")).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenAnswer(i -> i.getArgument(0));
        when(contractRepository.save(any(Contract.class))).thenAnswer(i -> i.getArgument(0));

        orchestrator.handleDomainEvent(event);

        verify(signatureService, times(1)).createSignatureSession(any(), any());
    }

    @Test
    @DisplayName("Deve processar CONTRACT_SIGNED e gerar O.S. com dimensionamento de materiais FTTH")
    void testHandleContractSigned() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("contractId", contractId.toString());
        payload.put("customerId", customerId.toString());

        DomainEvent event = GenericDomainEvent.builder()
                .eventId(UuidCreatorUtils.generateUuidV7())
                .eventType("CONTRACT_SIGNED")
                .payload(payload)
                .build();

        Contract contract = Contract.builder()
                .id(contractId)
                .customerId(customerId)
                .saleId(saleId)
                .contractNumber("CTR-888")
                .status(Contract.ContractStatus.DRAFT)
                .build();

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(saleRepository.findById(saleId)).thenReturn(Optional.of(Sale.builder().id(saleId).build()));
        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(i -> {
            WorkOrder wo = i.getArgument(0);
            wo.setId(UuidCreatorUtils.generateUuidV7());
            return wo;
        });
        when(demandService.generateDemandForWorkOrder(any())).thenReturn(
                InstallationMaterialDemand.builder().id(UuidCreatorUtils.generateUuidV7()).estimatedDropMeters(55).build()
        );

        orchestrator.handleDomainEvent(event);

        verify(demandService, times(1)).generateDemandForWorkOrder(any());
        verify(domainEventPublisher, times(1)).publish(any());
    }
}
