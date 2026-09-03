package br.dev.xb.isperp.service.financial;

import br.dev.xb.isperp.dto.financial.AccountingMethod;
import br.dev.xb.isperp.dto.financial.DeleveragingProjectionDto;
import br.dev.xb.isperp.dto.financial.DreReportDto;
import br.dev.xb.isperp.dto.financial.SimulationRequest;
import br.dev.xb.isperp.dto.financial.SimulationResponse;
import br.dev.xb.isperp.entity.Contract;
import br.dev.xb.isperp.entity.financial.ExpenseInstallment;
import br.dev.xb.isperp.entity.financial.PayableStatus;
import br.dev.xb.isperp.repository.ContractRepository;
import br.dev.xb.isperp.repository.financial.ExpenseInstallmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleveragingEngineServiceTest {

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private ExpenseInstallmentRepository installmentRepository;

    @Mock
    private DreReportService dreReportService;

    private DeleveragingEngineService engineService;

    @BeforeEach
    void setUp() {
        engineService = new DeleveragingEngineService(contractRepository, installmentRepository, dreReportService);
    }

    @Test
    @DisplayName("Deve projetar os 36 meses com precisão e identificar os 3 números sagrados do provedor")
    void shouldProject36MonthsAndIdentifyKeyMetrics() {
        // Base de contratos: MRR de R$ 50.000,00
        Contract c1 = Contract.builder().monthlyFee(new BigDecimal("50000.00")).status(Contract.ContractStatus.ACTIVE).build();
        when(contractRepository.findByStatus(Contract.ContractStatus.ACTIVE)).thenReturn(List.of(c1));

        // DRE do mês passado com OPEX de R$ 20.000,00 e EBITDA de R$ 30.000,00
        DreReportDto mockDre = DreReportDto.builder()
                .ebitda(new BigDecimal("30000.00"))
                .totalOpex(new BigDecimal("20000.00"))
                .build();
        when(dreReportService.generateDre(any(), any(), any(AccountingMethod.class))).thenReturn(mockDre);

        // Parcelas de CAPEX pesadas nos primeiros 6 meses (R$ 15.000/mês)
        LocalDate startMonth = LocalDate.now().withDayOfMonth(1);
        ExpenseInstallment inst1 = ExpenseInstallment.builder()
                .dueDate(startMonth.plusMonths(1))
                .amount(new BigDecimal("15000.00"))
                .status(PayableStatus.PENDING)
                .build();

        when(installmentRepository.findByStatusOrderByDueDateAsc(PayableStatus.PENDING)).thenReturn(List.of(inst1));

        DeleveragingProjectionDto projection = engineService.calculate36MonthsProjection();

        assertThat(projection.getCurrentMrr()).isEqualByComparingTo(new BigDecimal("50000.00"));
        assertThat(projection.getTimeline()).hasSize(36);
        assertThat(projection.getWorstMonthYearMonth()).isNotNull();
        assertThat(projection.getBreakEvenYearMonth()).isNotNull();
    }

    @Test
    @DisplayName("Simulador 'E Se...?' deve aprovar novo investimento quando o caixa suportar as parcelas")
    void shouldSimulateFeasibleInvestment() {
        Contract c1 = Contract.builder().monthlyFee(new BigDecimal("100000.00")).status(Contract.ContractStatus.ACTIVE).build();
        when(contractRepository.findByStatus(Contract.ContractStatus.ACTIVE)).thenReturn(List.of(c1));

        DreReportDto mockDre = DreReportDto.builder()
                .ebitda(new BigDecimal("60000.00"))
                .totalOpex(new BigDecimal("40000.00"))
                .build();
        when(dreReportService.generateDre(any(), any(), any(AccountingMethod.class))).thenReturn(mockDre);
        when(installmentRepository.findByStatusOrderByDueDateAsc(PayableStatus.PENDING)).thenReturn(List.of());

        // Simula compra de máquina OTDR de R$ 24.000 em 12x de R$ 2.000
        SimulationRequest request = SimulationRequest.builder()
                .description("Máquina OTDR de Precisão")
                .totalAmount(new BigDecimal("24000.00"))
                .installmentsCount(12)
                .firstDueDate(LocalDate.now().plusMonths(1))
                .build();

        SimulationResponse response = engineService.simulateNewInvestment(request);

        assertThat(response.getFeasible()).isTrue();
        assertThat(response.getMonthlyInstallmentAmount()).isEqualByComparingTo(new BigDecimal("2000.00"));
        assertThat(response.getRiskAnalysisSummary()).contains("Simulação APROVADA");
    }
}
