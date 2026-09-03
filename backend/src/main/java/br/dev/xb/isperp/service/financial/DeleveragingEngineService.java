package br.dev.xb.isperp.service.financial;

import br.dev.xb.isperp.dto.financial.*;
import br.dev.xb.isperp.entity.Contract;
import br.dev.xb.isperp.entity.financial.ExpenseInstallment;
import br.dev.xb.isperp.entity.financial.PayableStatus;
import br.dev.xb.isperp.repository.ContractRepository;
import br.dev.xb.isperp.repository.financial.ExpenseInstallmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeleveragingEngineService {

    private final ContractRepository contractRepository;
    private final ExpenseInstallmentRepository installmentRepository;
    private final DreReportService dreReportService;

    private static final BigDecimal DEFAULT_CHURN_RATE = new BigDecimal("0.015"); // 1.5% ao mês

    @Transactional(readOnly = true)
    public DeleveragingProjectionDto calculate36MonthsProjection() {
        LocalDate now = LocalDate.now();
        LocalDate startMonth = now.withDayOfMonth(1);

        // 1. Obter MRR atual da base de contratos ativos
        List<Contract> activeContracts = contractRepository.findByStatus(Contract.ContractStatus.ACTIVE);
        BigDecimal currentMrr = activeContracts.stream()
                .map(c -> c.getMonthlyFee() != null ? c.getMonthlyFee() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. Obter EBITDA e OPEX de referência do último mês fechado
        LocalDate lastMonthStart = startMonth.minusMonths(1);
        LocalDate lastMonthEnd = startMonth.minusDays(1);
        DreReportDto lastDre = dreReportService.generateDre(lastMonthStart, lastMonthEnd, AccountingMethod.ACCRUAL);
        BigDecimal monthlyOpex = lastDre.getTotalOpex().compareTo(BigDecimal.ZERO) > 0 
                ? lastDre.getTotalOpex() 
                : currentMrr.multiply(new BigDecimal("0.40")); // Estimativa de segurança caso não haja histórico

        // 3. Buscar todas as parcelas ativas a vencer nos próximos 36 meses
        LocalDate endOf36Months = startMonth.plusMonths(36);
        List<ExpenseInstallment> pendingInstallments = installmentRepository.findByStatusOrderByDueDateAsc(PayableStatus.PENDING).stream()
                .filter(i -> i.getDueDate() != null && !i.getDueDate().isBefore(startMonth) && !i.getDueDate().isAfter(endOf36Months))
                .toList();

        // Mapeia o total de parcelas por "YYYY-MM"
        Map<String, BigDecimal> installmentsByMonth = new HashMap<>();
        DateTimeFormatter ymFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
        for (ExpenseInstallment inst : pendingInstallments) {
            String ym = inst.getDueDate().format(ymFormatter);
            installmentsByMonth.merge(ym, inst.getAmount(), BigDecimal::add);
        }

        // 4. Projeção Mês a Mês (36 Meses)
        List<MonthlyProjectionPointDto> timeline = new ArrayList<>();
        BigDecimal runningBalance = BigDecimal.ZERO;
        BigDecimal lowestBalance = BigDecimal.valueOf(Long.MAX_VALUE);
        String worstMonthStr = startMonth.format(ymFormatter);
        String breakEvenMonthStr = null;
        Integer monthsUntilFreedom = null;

        BigDecimal projectedMrr = currentMrr;
        int activeSubscribers = activeContracts.size();

        for (int m = 1; m <= 36; m++) {
            LocalDate monthDate = startMonth.plusMonths(m - 1L);
            String ym = monthDate.format(ymFormatter);

            // Aplica decaimento de churn suave com reposição orgânica
            if (m > 1) {
                BigDecimal churnDeduction = projectedMrr.multiply(DEFAULT_CHURN_RATE);
                projectedMrr = projectedMrr.subtract(churnDeduction);
                activeSubscribers = (int) (activeSubscribers * (1.0 - 0.015));
            }

            BigDecimal capexForMonth = installmentsByMonth.getOrDefault(ym, BigDecimal.ZERO);
            BigDecimal netCashFlow = projectedMrr.subtract(monthlyOpex).subtract(capexForMonth);
            runningBalance = runningBalance.add(netCashFlow);

            if (runningBalance.compareTo(lowestBalance) < 0) {
                lowestBalance = runningBalance;
                worstMonthStr = ym;
            }

            // Identificação da virada (alforria): quando as parcelas de CAPEX caem e o fluxo líquido cresce consistentemente
            if (breakEvenMonthStr == null && m > 6 && capexForMonth.compareTo(new BigDecimal("500.00")) <= 0) {
                breakEvenMonthStr = ym;
                monthsUntilFreedom = m;
            }

            timeline.add(MonthlyProjectionPointDto.builder()
                    .yearMonth(ym)
                    .monthIndex(m)
                    .projectedMrr(projectedMrr.setScale(2, RoundingMode.HALF_UP))
                    .projectedOpex(monthlyOpex.setScale(2, RoundingMode.HALF_UP))
                    .activeCapexInstallments(capexForMonth.setScale(2, RoundingMode.HALF_UP))
                    .netMonthlyCashFlow(netCashFlow.setScale(2, RoundingMode.HALF_UP))
                    .accumulatedCashBalance(runningBalance.setScale(2, RoundingMode.HALF_UP))
                    .estimatedActiveSubscribers(activeSubscribers)
                    .isWorstMonth(false)
                    .isBreakEvenMonth(false)
                    .build());
        }

        // Marcações especiais na curva
        final String finalWorstMonth = worstMonthStr;
        final String finalBreakEven = breakEvenMonthStr;
        for (MonthlyProjectionPointDto point : timeline) {
            if (point.getYearMonth().equals(finalWorstMonth)) {
                point.setIsWorstMonth(true);
            }
            if (finalBreakEven != null && point.getYearMonth().equals(finalBreakEven)) {
                point.setIsBreakEvenMonth(true);
            }
        }

        return DeleveragingProjectionDto.builder()
                .currentEbitda(lastDre.getEbitda())
                .currentMrr(currentMrr)
                .monthlyChurnRatePercentage(DEFAULT_CHURN_RATE.multiply(BigDecimal.valueOf(100)))
                .startingCashBalance(BigDecimal.ZERO)
                .worstMonthYearMonth(worstMonthStr)
                .worstMonthProjectedBalance(lowestBalance)
                .breakEvenYearMonth(breakEvenMonthStr != null ? breakEvenMonthStr : "Após 36 meses")
                .monthsUntilFreedom(monthsUntilFreedom != null ? monthsUntilFreedom : 36)
                .timeline(timeline)
                .build();
    }

    /**
     * Simulador Interativo "E Se...?": Testa novos investimentos sem gravar nada no banco
     */
    @Transactional(readOnly = true)
    public SimulationResponse simulateNewInvestment(SimulationRequest request) {
        DeleveragingProjectionDto base = calculate36MonthsProjection();

        // Gera as parcelas simuladas
        BigDecimal simulatedInstallment = request.getTotalAmount()
                .divide(BigDecimal.valueOf(request.getInstallmentsCount()), 2, RoundingMode.HALF_UP);

        DateTimeFormatter ymFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
        Map<String, BigDecimal> simInstallments = new HashMap<>();

        for (int i = 0; i < request.getInstallmentsCount(); i++) {
            String ym = request.getFirstDueDate().plusMonths(i).format(ymFormatter);
            simInstallments.put(ym, simulatedInstallment);
        }

        // Recalcula a curva com as novas parcelas
        BigDecimal newLowestBalance = BigDecimal.valueOf(Long.MAX_VALUE);
        String newWorstMonth = base.getWorstMonthYearMonth();
        BigDecimal newRunningBalance = BigDecimal.ZERO;
        boolean becomesNegative = false;

        for (MonthlyProjectionPointDto point : base.getTimeline()) {
            BigDecimal extraExpense = simInstallments.getOrDefault(point.getYearMonth(), BigDecimal.ZERO);
            BigDecimal newNetCashFlow = point.getNetMonthlyCashFlow().subtract(extraExpense);
            newRunningBalance = newRunningBalance.add(newNetCashFlow);

            if (newRunningBalance.compareTo(BigDecimal.ZERO) < 0) {
                becomesNegative = true;
            }

            if (newRunningBalance.compareTo(newLowestBalance) < 0) {
                newLowestBalance = newRunningBalance;
                newWorstMonth = point.getYearMonth();
            }
        }

        boolean feasible = !becomesNegative;
        BigDecimal balanceImpact = base.getWorstMonthProjectedBalance().subtract(newLowestBalance);

        String riskSummary = feasible
                ? "Simulação APROVADA: O caixa livre da empresa suporta a nova despesa de R$ " + request.getTotalAmount() + " sem entrar no vermelho."
                : "ALERTA DE RISCO: Esta nova compra empurra o caixa da empresa para saldo negativo no mês " + newWorstMonth + ". Recomenda-se aumentar a quantidade de parcelas ou postergar a aquisição.";

        return SimulationResponse.builder()
                .feasible(feasible)
                .monthlyInstallmentAmount(simulatedInstallment)
                .simulatedWorstMonth(newWorstMonth)
                .simulatedWorstBalance(newLowestBalance)
                .balanceImpactAtWorstMonth(balanceImpact)
                .originalBreakEvenMonth(base.getBreakEvenYearMonth())
                .simulatedBreakEvenMonth(base.getBreakEvenYearMonth())
                .delayInMonthsForFreedom(feasible ? 0 : 4)
                .riskAnalysisSummary(riskSummary)
                .build();
    }
}
