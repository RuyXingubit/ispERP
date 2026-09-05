package br.dev.xb.isperp.service.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import br.dev.xb.isperp.entity.Contract;
import br.dev.xb.isperp.entity.WorkOrder;
import br.dev.xb.isperp.event.GenericDomainEvent;
import br.dev.xb.isperp.service.ContractService;
import br.dev.xb.isperp.service.IdempotencyService;
import br.dev.xb.isperp.service.InstallationDemandService;
import br.dev.xb.isperp.service.WorkOrderService;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class WorkOrderEventConsumerTest {

    @Mock
    private WorkOrderService workOrderService;

    @Mock
    private ContractService contractService;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private InstallationDemandService installationDemandService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private WorkOrderEventConsumer workOrderEventConsumer;

    private UUID contractId;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        contractId = UuidCreatorUtils.generateUuidV7();
        customerId = UuidCreatorUtils.generateUuidV7();
    }

    @Test
    @DisplayName("Deve criar O.S. pendente ao consumir CONTRACT_CREATED")
    void shouldCreateWorkOrderOnContractCreated() {
        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(2);
            action.run();
            return true;
        }).when(idempotencyService).executeIdempotent(any(), any(), any());

        Map<String, Object> payload = new HashMap<>();
        payload.put("contractId", contractId.toString());
        payload.put("customerId", customerId.toString());
        payload.put("installationAddress", "Av. Brasil, 1000");
        payload.put("city", "São Paulo");
        payload.put("state", "SP");

        GenericDomainEvent event = GenericDomainEvent.builder()
                .eventId(UuidCreatorUtils.generateUuidV7())
                .eventType("CONTRACT_CREATED")
                .aggregateType("Contract")
                .aggregateId(contractId.toString())
                .payload(payload)
                .build();

        WorkOrder mockWorkOrder = WorkOrder.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .contractId(contractId)
                .customerId(customerId)
                .build();
        when(workOrderService.createInitialInstallationWorkOrder(any(), any(), any())).thenReturn(mockWorkOrder);

        workOrderEventConsumer.handleDomainEvent(event);

        verify(workOrderService, times(1)).createInitialInstallationWorkOrder(eq(contractId), eq(customerId), anyString());
        verify(installationDemandService, times(1)).generateDemandForWorkOrder(mockWorkOrder.getId());
    }

    @Test
    @DisplayName("Deve ativar contrato ao consumir WORK_ORDER_COMPLETED")
    void shouldActivateContractOnWorkOrderCompleted() {
        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(2);
            action.run();
            return true;
        }).when(idempotencyService).executeIdempotent(any(), any(), any());

        Map<String, Object> payload = new HashMap<>();
        payload.put("contractId", contractId.toString());
        payload.put("workOrderId", UuidCreatorUtils.generateUuidV7().toString());

        GenericDomainEvent event = GenericDomainEvent.builder()
                .eventId(UuidCreatorUtils.generateUuidV7())
                .eventType("WORK_ORDER_COMPLETED")
                .aggregateType("WorkOrder")
                .aggregateId(contractId.toString())
                .payload(payload)
                .build();

        workOrderEventConsumer.handleDomainEvent(event);

        verify(contractService, times(1)).updateStatus(contractId, Contract.ContractStatus.ACTIVE);
    }
}
