package br.dev.xb.isperp.mapper;

import br.dev.xb.isperp.api.dto.CompanyResponse;
import br.dev.xb.isperp.api.dto.PlanCreateRequest;
import br.dev.xb.isperp.api.dto.PlanResponse;
import br.dev.xb.isperp.api.dto.SaleResponse;
import br.dev.xb.isperp.api.dto.SaleStatusEnum;
import br.dev.xb.isperp.dto.ContractDTO;
import br.dev.xb.isperp.dto.WorkOrderDTO;
import br.dev.xb.isperp.entity.Company;
import br.dev.xb.isperp.entity.Contract;
import br.dev.xb.isperp.entity.Plan;
import br.dev.xb.isperp.entity.Sale;
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
    private final CompanyMapper companyMapper = Mappers.getMapper(CompanyMapper.class);
    private final SaleMapper saleMapper = Mappers.getMapper(SaleMapper.class);

    @Test
    @DisplayName("Deve mapear Plan para PlanResponse e vice-versa corretamente com MapStruct")
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

        PlanResponse response = planMapper.toResponse(plan);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(planId);
        assertThat(response.getName()).isEqualTo("Fibra 500 Mega");
        assertThat(response.getDownloadSpeed()).isEqualTo(500);
        assertThat(response.getPrice()).isEqualTo(99.90);

        PlanCreateRequest createRequest = new PlanCreateRequest("Fibra 500 Mega", 500, 250, 99.90);
        Plan entityFromDto = planMapper.toEntity(createRequest);
        assertThat(entityFromDto).isNotNull();
        assertThat(entityFromDto.getName()).isEqualTo("Fibra 500 Mega");
        assertThat(entityFromDto.getDownloadSpeed()).isEqualTo(500);

        List<PlanResponse> responseList = planMapper.toResponseList(List.of(plan));
        assertThat(responseList).hasSize(1);
        assertThat(responseList.get(0).getName()).isEqualTo("Fibra 500 Mega");
    }

    @Test
    @DisplayName("Deve mapear Company para CompanyResponse e vice-versa corretamente")
    void shouldMapCompanyToResponse() {
        UUID companyId = UUID.randomUUID();
        Company company = Company.builder()
                .id(companyId)
                .name("ISP Matriz")
                .document("12.345.678/0001-90")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        CompanyResponse response = companyMapper.toResponse(company);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(companyId);
        assertThat(response.getName()).isEqualTo("ISP Matriz");
        assertThat(response.getDocument()).isEqualTo("12.345.678/0001-90");
    }

    @Test
    @DisplayName("Deve mapear Sale para SaleResponse e converter status para SaleStatusEnum")
    void shouldMapSaleToResponse() {
        UUID saleId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        Sale sale = Sale.builder()
                .id(saleId)
                .planId(planId)
                .customerName("Ana Maria")
                .customerCpf("12345678900")
                .customerPhone("11987654321")
                .installationAddress("Av Paulista, 1000")
                .city("São Paulo")
                .state("SP")
                .zipCode("01310100")
                .status(Sale.SaleStatus.SUBMITTED)
                .createdAt(LocalDateTime.now())
                .build();

        SaleResponse response = saleMapper.toResponse(sale);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(saleId);
        assertThat(response.getCustomerName()).isEqualTo("Ana Maria");
        assertThat(response.getStatus()).isEqualTo(SaleStatusEnum.SUBMITTED);
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
                .status(Contract.ContractStatus.ACTIVE)
                .installationAddress("Rua dos Ipês, 456")
                .city("São Paulo")
                .state("SP")
                .zipCode("01000-000")
                .monthlyFee(new BigDecimal("129.90"))
                .dueDay(10)
                .createdAt(LocalDateTime.now())
                .build();

        ContractDTO dto = contractMapper.toDto(contract);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(contractId);
        assertThat(dto.getCustomerId()).isEqualTo(customerId);
        assertThat(dto.getPlanId()).isEqualTo(planId);
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
