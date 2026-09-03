package br.dev.xb.isperp.service.financial;

import br.dev.xb.isperp.dto.financial.WorkOrderFeeAuditRequest;
import br.dev.xb.isperp.dto.financial.WorkOrderFeeDto;
import br.dev.xb.isperp.dto.financial.WorkOrderFeeWaiverRequest;
import br.dev.xb.isperp.entity.Customer;
import br.dev.xb.isperp.entity.User;
import br.dev.xb.isperp.entity.UserRole;
import br.dev.xb.isperp.entity.WorkOrder;
import br.dev.xb.isperp.entity.financial.FeeStatus;
import br.dev.xb.isperp.event.DomainEvent;
import br.dev.xb.isperp.mapper.FinancialAccountMapper;
import br.dev.xb.isperp.repository.UserRepository;
import br.dev.xb.isperp.repository.WorkOrderRepository;
import br.dev.xb.isperp.service.DomainEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkOrderFeeServiceTest {

    @Mock
    private WorkOrderRepository workOrderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private br.dev.xb.isperp.repository.CustomerRepository customerRepository;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    private final FinancialAccountMapper mapper = Mappers.getMapper(FinancialAccountMapper.class);

    private WorkOrderFeeService workOrderFeeService;

    private User attendant;
    private User managerCfo;
    private UUID attendantId;
    private UUID managerId;

    @BeforeEach
    void setUp() {
        workOrderFeeService = new WorkOrderFeeService(
                workOrderRepository,
                userRepository,
                customerRepository,
                domainEventPublisher,
                mapper
        );

        attendantId = UUID.randomUUID();
        attendant = User.builder()
                .id(attendantId)
                .name("Maria Atendente")
                .role(UserRole.ATTENDANT)
                .build();

        managerId = UUID.randomUUID();
        managerCfo = User.builder()
                .id(managerId)
                .name("Roberto CFO")
                .role(UserRole.CFO)
                .build();
    }

    @Test
    @DisplayName("Atendente solicita isenção de taxa de O.S. com justificativa de retenção")
    void shouldRequestFeeWaiverSuccessfully() {
        UUID woId = UUID.randomUUID();
        WorkOrder workOrder = WorkOrder.builder()
                .id(woId)
                .standardFeeAmount(new BigDecimal("100.00"))
                .feeStatus(FeeStatus.BILLABLE)
                .build();

        when(workOrderRepository.findById(woId)).thenReturn(Optional.of(workOrder));
        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(i -> i.getArgument(0));

        WorkOrderFeeWaiverRequest request = WorkOrderFeeWaiverRequest.builder()
                .workOrderId(woId)
                .waiverReason("Cliente ameaçou cancelamento por insatisfação com taxa de mudança de endereço")
                .build();

        WorkOrderFeeDto result = workOrderFeeService.requestWaiver(attendantId, request);

        assertThat(result.getFeeStatus()).isEqualTo(FeeStatus.PENDING_WAIVER_APPROVAL);
        assertThat(result.getWaiverReason()).contains("Cliente ameaçou cancelamento");
    }

    @Test
    @DisplayName("Apenas gestor/CFO aprova isenção e sistema dispara evento com dados do cliente para WhatsApp oficial")
    void shouldAuditWaiverAndPublishAntiFraudDomainEvent() {
        UUID woId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Customer customer = Customer.builder()
                .id(customerId)
                .name("João Assinante")
                .phone("11999998888")
                .build();

        WorkOrder workOrder = WorkOrder.builder()
                .id(woId)
                .customerId(customerId)
                .contractId(UUID.randomUUID())
                .standardFeeAmount(new BigDecimal("100.00"))
                .feeStatus(FeeStatus.PENDING_WAIVER_APPROVAL)
                .waiverReason("Retenção")
                .build();

        when(userRepository.findById(managerId)).thenReturn(Optional.of(managerCfo));
        when(workOrderRepository.findById(woId)).thenReturn(Optional.of(workOrder));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(i -> i.getArgument(0));

        WorkOrderFeeAuditRequest auditRequest = WorkOrderFeeAuditRequest.builder()
                .approved(true)
                .notes("Aprovado em caráter de fidelidade")
                .build();

        WorkOrderFeeDto result = workOrderFeeService.auditWaiver(managerId, woId, auditRequest);

        assertThat(result.getFeeStatus()).isEqualTo(FeeStatus.WAIVED_APPROVED);

        // Verifica que o evento WORK_ORDER_FEE_WAIVED foi publicado
        ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(domainEventPublisher).publish(eventCaptor.capture());
        DomainEvent published = eventCaptor.getValue();
        assertThat(published.getEventType()).isEqualTo("WORK_ORDER_FEE_WAIVED");
    }

    @Test
    @DisplayName("Colaborador comum sem alçada de gestão não pode aprovar isenção de taxas")
    void shouldDenyWaiverApprovalByNonManager() {
        UUID woId = UUID.randomUUID();
        when(userRepository.findById(attendantId)).thenReturn(Optional.of(attendant));

        WorkOrderFeeAuditRequest auditRequest = WorkOrderFeeAuditRequest.builder()
                .approved(true)
                .build();

        assertThatThrownBy(() -> workOrderFeeService.auditWaiver(attendantId, woId, auditRequest))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Acesso negado");
    }
}
