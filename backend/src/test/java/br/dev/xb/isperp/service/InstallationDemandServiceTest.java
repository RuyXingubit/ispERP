package br.dev.xb.isperp.service;

import br.dev.xb.isperp.entity.*;
import br.dev.xb.isperp.mapper.InstallationMaterialDemandMapper;
import br.dev.xb.isperp.repository.*;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
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
class InstallationDemandServiceTest {

    @Mock
    private InstallationMaterialDemandRepository demandRepository;

    @Mock
    private WorkOrderRepository workOrderRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private FtthCtoRepository ctoRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    private final InstallationMaterialDemandMapper demandMapper = Mappers.getMapper(InstallationMaterialDemandMapper.class);

    private InstallationDemandService demandService;

    private UUID workOrderId;
    private UUID contractId;
    private UUID customerId;
    private UUID planId;
    private UUID ctoId;

    @BeforeEach
    void setUp() {
        demandService = new InstallationDemandService(
                demandRepository,
                workOrderRepository,
                contractRepository,
                customerRepository,
                planRepository,
                ctoRepository,
                warehouseRepository,
                demandMapper,
                inventoryService,
                domainEventPublisher
        );

        workOrderId = UuidCreatorUtils.generateUuidV7();
        contractId = UuidCreatorUtils.generateUuidV7();
        customerId = UuidCreatorUtils.generateUuidV7();
        planId = UuidCreatorUtils.generateUuidV7();
        ctoId = UuidCreatorUtils.generateUuidV7();
    }

    @Test
    @DisplayName("Deve dimensionar metragem do drop óptico com folga técnica e kit de instalação FTTH")
    void testGenerateDemandForWorkOrder() {
        WorkOrder wo = WorkOrder.builder()
                .id(workOrderId)
                .contractId(contractId)
                .customerId(customerId)
                .build();

        Contract contract = Contract.builder()
                .id(contractId)
                .customerId(customerId)
                .planId(planId)
                .contractNumber("CTR-12345")
                .installationAddress("Av. Nazaré, 500")
                .build();

        Customer customer = Customer.builder()
                .id(customerId)
                .name("Carlos Alberto")
                .latitude(BigDecimal.valueOf(-1.4550))
                .longitude(BigDecimal.valueOf(-48.4900))
                .build();

        Plan plan = Plan.builder()
                .id(planId)
                .name("Fibra 600 Mega")
                .downloadSpeed(600)
                .build();

        FtthCto cto = FtthCto.builder()
                .id(ctoId)
                .name("CTO-NZ-01")
                .latitude(BigDecimal.valueOf(-1.4560))
                .longitude(BigDecimal.valueOf(-48.4910))
                .build();

        when(workOrderRepository.findById(workOrderId)).thenReturn(Optional.of(wo));
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(ctoRepository.findAll()).thenReturn(List.of(cto));
        when(demandRepository.findByWorkOrderId(workOrderId)).thenReturn(Optional.empty());
        when(demandRepository.save(any(InstallationMaterialDemand.class))).thenAnswer(i -> i.getArgument(0));

        InstallationMaterialDemand result = demandService.generateDemandForWorkOrder(workOrderId);

        assertThat(result).isNotNull();
        assertThat(result.getEstimatedDropMeters()).isGreaterThanOrEqualTo(30);
        assertThat(result.getOnuModelRequired()).contains("ONT Wi-Fi");
        assertThat(result.getFastConnectorsCount()).isEqualTo(2);
        assertThat(result.getPtoRosetteCount()).isEqualTo(1);
        assertThat(result.getStatus()).isEqualTo(MaterialDemandStatus.PENDING_ALLOCATION);
    }

    @Test
    @DisplayName("Deve confirmar materiais no almoxarifado central com status ALLOCATED_CENTRAL e emitir evento")
    void testConfirmStockAllocation() {
        UUID centralWarehouseId = UuidCreatorUtils.generateUuidV7();
        Warehouse central = Warehouse.builder()
                .id(centralWarehouseId)
                .name("Depósito Central Altamira")
                .active(true)
                .build();

        InstallationMaterialDemand existingDemand = InstallationMaterialDemand.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .workOrderId(workOrderId)
                .contractId(contractId)
                .estimatedDropMeters(60)
                .status(MaterialDemandStatus.PENDING_ALLOCATION)
                .build();

        when(demandRepository.findByWorkOrderId(workOrderId)).thenReturn(Optional.of(existingDemand));
        when(demandRepository.save(any(InstallationMaterialDemand.class))).thenAnswer(i -> i.getArgument(0));

        var response = demandService.confirmStockAllocation(workOrderId, centralWarehouseId);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(MaterialDemandStatus.ALLOCATED_CENTRAL);
        assertThat(response.getAllocatedWarehouseId()).isEqualTo(centralWarehouseId);
        verify(demandRepository, times(1)).save(any(InstallationMaterialDemand.class));
        verify(domainEventPublisher, times(1)).publish(any());
        verify(inventoryService, times(1)).checkAndReserveInstallationMaterials(contractId);
    }
}
