package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.TechnicianDispatchCandidateResponse;
import br.dev.xb.isperp.entity.*;
import br.dev.xb.isperp.repository.*;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TechnicianDispatchServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private SerializedAssetRepository serializedAssetRepository;

    @Mock
    private WorkOrderRepository workOrderRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private InstallationMaterialDemandRepository demandRepository;

    @Mock
    private InstallationDemandService demandService;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    private TechnicianDispatchService dispatchService;

    private UUID workOrderId;
    private UUID technicianId;
    private UUID warehouseId;

    @BeforeEach
    void setUp() {
        dispatchService = new TechnicianDispatchService(
                userRepository,
                warehouseRepository,
                serializedAssetRepository,
                workOrderRepository,
                contractRepository,
                customerRepository,
                demandRepository,
                demandService,
                domainEventPublisher
        );

        workOrderId = UuidCreatorUtils.generateUuidV7();
        technicianId = UuidCreatorUtils.generateUuidV7();
        warehouseId = UuidCreatorUtils.generateUuidV7();
    }

    @Test
    @DisplayName("Deve ranquear técnicos por estoque em veículo e proximidade GPS")
    void testListCandidatesForWorkOrder() {
        WorkOrder wo = WorkOrder.builder()
                .id(workOrderId)
                .contractId(UuidCreatorUtils.generateUuidV7())
                .customerId(UuidCreatorUtils.generateUuidV7())
                .build();

        Contract contract = Contract.builder()
                .id(wo.getContractId())
                .customerId(wo.getCustomerId())
                .build();

        Customer customer = Customer.builder()
                .id(wo.getCustomerId())
                .latitude(BigDecimal.valueOf(-1.4550))
                .longitude(BigDecimal.valueOf(-48.4900))
                .build();

        InstallationMaterialDemand demand = InstallationMaterialDemand.builder()
                .estimatedDropMeters(60)
                .build();

        User tech1 = User.builder().id(technicianId).name("Técnico Silva").role(UserRole.TECHNICIAN).build();
        Warehouse v1 = Warehouse.builder().id(warehouseId).name("Veículo 01 - Silva").responsibleUserId(technicianId).build();

        when(workOrderRepository.findById(workOrderId)).thenReturn(Optional.of(wo));
        when(contractRepository.findById(wo.getContractId())).thenReturn(Optional.of(contract));
        when(customerRepository.findById(wo.getCustomerId())).thenReturn(Optional.of(customer));
        when(demandRepository.findByWorkOrderId(workOrderId)).thenReturn(Optional.of(demand));
        when(userRepository.findByRole(UserRole.TECHNICIAN)).thenReturn(List.of(tech1));
        when(warehouseRepository.findByResponsibleUserId(technicianId)).thenReturn(Optional.of(v1));
        when(serializedAssetRepository.findByCurrentWarehouseIdAndStatus(warehouseId, SerializedAsset.AssetStatus.DISPONIVEL_DEPOSITO)).thenReturn(List.of(
                SerializedAsset.builder().serialNumber("HWTC1234").status(SerializedAsset.AssetStatus.DISPONIVEL_DEPOSITO).build()
        ));

        List<TechnicianDispatchCandidateResponse> candidates = dispatchService.listCandidatesForWorkOrder(workOrderId);

        assertThat(candidates).isNotEmpty();
        TechnicianDispatchCandidateResponse topCandidate = candidates.get(0);
        assertThat(topCandidate.getTechnicianName()).isEqualTo("Técnico Silva");
        assertThat(topCandidate.getHasCompleteKit()).isTrue();
        assertThat(topCandidate.getRecommendedScore()).isGreaterThan(50.0);
    }

    @Test
    @DisplayName("Deve despachar O.S. e alocar materiais na custódia do veículo")
    void testDispatchWorkOrder() {
        WorkOrder wo = WorkOrder.builder()
                .id(workOrderId)
                .contractId(UuidCreatorUtils.generateUuidV7())
                .customerId(UuidCreatorUtils.generateUuidV7())
                .status(WorkOrder.WorkOrderStatus.PENDING_SCHEDULE)
                .build();

        User tech = User.builder().id(technicianId).name("Técnico Silva").build();
        Warehouse vehicle = Warehouse.builder().id(warehouseId).name("Veículo Silva").build();
        InstallationMaterialDemand demand = InstallationMaterialDemand.builder().build();

        when(workOrderRepository.findById(workOrderId)).thenReturn(Optional.of(wo));
        when(userRepository.findById(technicianId)).thenReturn(Optional.of(tech));
        when(warehouseRepository.findByResponsibleUserId(technicianId)).thenReturn(Optional.of(vehicle));
        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(i -> i.getArgument(0));
        when(demandRepository.findByWorkOrderId(workOrderId)).thenReturn(Optional.of(demand));

        WorkOrder dispatched = dispatchService.dispatchWorkOrder(workOrderId, technicianId);

        assertThat(dispatched.getStatus()).isEqualTo(WorkOrder.WorkOrderStatus.SCHEDULED);
        assertThat(dispatched.getTechnicianName()).isEqualTo("Técnico Silva");
        verify(domainEventPublisher, times(1)).publish(any());
    }
}
