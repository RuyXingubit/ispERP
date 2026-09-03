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
public class MonthlyProjectionPointDto {
    private String yearMonth; // Ex: "2026-10"
    private Integer monthIndex; // 1 a 36
    private BigDecimal projectedMrr;
    private BigDecimal projectedOpex;
    private BigDecimal activeCapexInstallments;
    private BigDecimal netMonthlyCashFlow;
    private BigDecimal accumulatedCashBalance;
    private Integer estimatedActiveSubscribers;
    private Boolean isWorstMonth;
    private Boolean isBreakEvenMonth;
}
