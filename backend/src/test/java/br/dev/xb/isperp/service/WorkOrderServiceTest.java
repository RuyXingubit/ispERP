package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.CompleteWorkOrderRequest;
import br.dev.xb.isperp.dto.ScheduleWorkOrderRequest;
import br.dev.xb.isperp.entity.WorkOrder;
import br.dev.xb.isperp.event.DomainEvent;
import br.dev.xb.isperp.repository.WorkOrderRepository;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class WorkOrderServiceTest {

    @Mock
    private WorkOrderRepository workOrderRepository;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @InjectMocks
    private WorkOrderService workOrderService;

    private WorkOrder sampleWorkOrder;
    private UUID workOrderId;

    @BeforeEach
    void setUp() {
        workOrderId = UuidCreatorUtils.generateUuidV7();
        sampleWorkOrder = WorkOrder.builder()
                .id(workOrderId)
                .contractId(UuidCreatorUtils.generateUuidV7())
                .customerId(UuidCreatorUtils.generateUuidV7())
                .type(WorkOrder.WorkOrderType.INSTALACAO)
                .status(WorkOrder.WorkOrderStatus.PENDING_SCHEDULE)
                .build();
    }

    @Test
    @DisplayName("Deve agendar O.S. com data, período e técnico")
    void shouldScheduleWorkOrder() {
        ScheduleWorkOrderRequest req = ScheduleWorkOrderRequest.builder()
                .scheduledDate(LocalDate.now().plusDays(2))
                .scheduledPeriod("MANHA")
                .technicianName("Carlos Técnico")
                .notes("Cliente disponível a partir das 9h")
                .build();

        when(workOrderRepository.findById(workOrderId)).thenReturn(Optional.of(sampleWorkOrder));
        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(i -> i.getArgument(0));

        WorkOrder scheduled = workOrderService.scheduleWorkOrder(workOrderId, req);

        assertEquals(WorkOrder.WorkOrderStatus.SCHEDULED, scheduled.getStatus());
        assertEquals("Carlos Técnico", scheduled.getTechnicianName());
        assertEquals("MANHA", scheduled.getScheduledPeriod());
    }

    @Test
    @DisplayName("Deve concluir O.S., registrar MAC/Serial/Sinal dBm e publicar WORK_ORDER_COMPLETED")
    void shouldCompleteWorkOrderAndPublishEvent() {
        CompleteWorkOrderRequest req = CompleteWorkOrderRequest.builder()
                .onuMac("AA:BB:CC:DD:EE:FF")
                .onuSerial("ZTEG12345678")
                .fiberSignalDbm(new BigDecimal("-19.45"))
                .notes("Instalação com sinal excelente e roteador configurado")
                .build();

        when(workOrderRepository.findById(workOrderId)).thenReturn(Optional.of(sampleWorkOrder));
        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(i -> i.getArgument(0));

        WorkOrder completed = workOrderService.completeWorkOrder(workOrderId, req);

        assertEquals(WorkOrder.WorkOrderStatus.COMPLETED, completed.getStatus());
        assertEquals("AA:BB:CC:DD:EE:FF", completed.getOnuMac());
        assertEquals("ZTEG12345678", completed.getOnuSerial());
        assertEquals(new BigDecimal("-19.45"), completed.getFiberSignalDbm());
        assertNotNull(completed.getCompletedAt());

        verify(domainEventPublisher, times(1)).publish(any(DomainEvent.class));
    }
}
