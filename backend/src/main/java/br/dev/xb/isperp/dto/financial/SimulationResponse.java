package br.dev.xb.isperp.dto.financial;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimulationResponse {
    private Boolean feasible; // Verdadeiro se o caixa livre não ficar negativo em nenhum dos 36 meses
    private BigDecimal monthlyInstallmentAmount;
    private String simulatedWorstMonth;
    private BigDecimal simulatedWorstBalance;
    private BigDecimal balanceImpactAtWorstMonth;
    private String originalBreakEvenMonth;
    private String simulatedBreakEvenMonth;
    private Integer delayInMonthsForFreedom;
    private String riskAnalysisSummary;
}
