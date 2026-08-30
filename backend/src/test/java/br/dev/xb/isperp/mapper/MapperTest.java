package br.dev.xb.isperp.mapper;

import br.dev.xb.isperp.dto.ContractDTO;
import br.dev.xb.isperp.dto.PlanDTO;
import br.dev.xb.isperp.dto.WorkOrderDTO;
import br.dev.xb.isperp.entity.Contract;
import br.dev.xb.isperp.entity.Plan;
import br.dev.xb.isperp.entity.WorkOrder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MapperTest {

    private final PlanMapper planMapper = Mappers.getMapper(PlanMapper.class);
    private final ContractMapper contractMapper = Mappers.getMapper(ContractMapper.class);
    private final WorkOrderMapper workOrderMapper = Mappers.getMapper(WorkOrderMapper.class);

    @Test
    @DisplayName("Deve mapear Plan para PlanDTO e vice-versa corretamente com MapStruct")
    void shouldMapPlanToDtoAndEntity() {
        UUID planId = UUID.randomUUID();
        Plan plan = Plan.builder()
                .id(planId)
                .name("Fibra 500 Mega")
                .downloadSpeed(500)
                .uploadSpeed(250)
                .price(new BigDecimal("99.90"))
                .description("Plano Residencial")
                .svaIncluded("Paramount+")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        PlanDTO dto = planMapper.toDto(plan);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(planId);
        assertThat(dto.getName()).isEqualTo("Fibra 500 Mega");
        assertThat(dto.getDownloadSpeed()).isEqualTo(500);
        assertThat(dto.getPrice()).isEqualByComparingTo("99.90");

        Plan entityFromDto = planMapper.toEntity(dto);
        assertThat(entityFromDto).isNotNull();
        assertThat(entityFromDto.getName()).isEqualTo("Fibra 500 Mega");
        assertThat(entityFromDto.getDownloadSpeed()).isEqualTo(500);

        List<PlanDTO> dtoList = planMapper.toDtoList(List.of(plan));
        assertThat(dtoList).hasSize(1);
        assertThat(dtoList.get(0).getName()).isEqualTo("Fibra 500 Mega");
    }

    @Test
    @DisplayName("Deve mapear Contract para ContractDTO corretamente")
    void shouldMapContractToDto() {
        UUID contractId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();

        Contract contract = Contract.builder()
                .id(contractId)
                .customerId(customerId)
                .planId(planId)
                .contractNumber("CTR-2026-001")
                .status(Contract.ContractStatus.ACTIVE)
                .monthlyFee(new BigDecimal("129.90"))
                .dueDay(15)
                .installationAddress("Rua das Flores, 123")
                .city("São Paulo")
                .state("SP")
                .zipCode("01000-000")
                .createdAt(LocalDateTime.now())
                .build();

        ContractDTO dto = contractMapper.toDto(contract);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(contractId);
        assertThat(dto.getCustomerId()).isEqualTo(customerId);
        assertThat(dto.getContractNumber()).isEqualTo("CTR-2026-001");
        assertThat(dto.getStatus()).isEqualTo(Contract.ContractStatus.ACTIVE);
        assertThat(dto.getMonthlyFee()).isEqualByComparingTo("129.90");
    }

    @Test
    @DisplayName("Deve mapear WorkOrder para WorkOrderDTO corretamente")
    void shouldMapWorkOrderToDto() {
        UUID workOrderId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        WorkOrder workOrder = WorkOrder.builder()
                .id(workOrderId)
                .contractId(contractId)
                .customerId(customerId)
                .type(WorkOrder.WorkOrderType.INSTALACAO)
                .status(WorkOrder.WorkOrderStatus.COMPLETED)
                .scheduledDate(LocalDate.now())
                .scheduledPeriod("MANHA")
                .technicianName("Carlos Silva")
                .onuMac("AA:BB:CC:DD:EE:FF")
                .onuSerial("ZTEGC123456")
                .fiberSignalDbm(new BigDecimal("-19.50"))
                .notes("Instalação com sinal excelente")
                .completedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        WorkOrderDTO dto = workOrderMapper.toDto(workOrder);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(workOrderId);
        assertThat(dto.getContractId()).isEqualTo(contractId);
        assertThat(dto.getTechnicianName()).isEqualTo("Carlos Silva");
        assertThat(dto.getOnuMac()).isEqualTo("AA:BB:CC:DD:EE:FF");
        assertThat(dto.getFiberSignalDbm()).isEqualByComparingTo("-19.50");
        assertThat(dto.getStatus()).isEqualTo(WorkOrder.WorkOrderStatus.COMPLETED);
    }
}
