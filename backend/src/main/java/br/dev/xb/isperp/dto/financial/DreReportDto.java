package br.dev.xb.isperp.dto.financial;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DreReportDto {
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private AccountingMethod accountingMethod;

    // 1. Receita Bruta
    private BigDecimal grossRevenue;

    // 2. Deduções e Impostos
    private BigDecimal taxDeductions;

    // 3. Receita Líquida
    private BigDecimal netRevenue;

    // 4. Custos Diretos de Interconexão e Trânsito IP
    private BigDecimal directCostsInterconnection;

    // 5. Margem de Contribuição
    private BigDecimal contributionMargin;

    // 6. OPEX Detalhado
    private BigDecimal opexHr;
    private BigDecimal opexPoles;
    private BigDecimal opexFleet;
    private BigDecimal opexMarketing;
    private BigDecimal opexAdmin;
    private BigDecimal totalOpex;

    // 7. EBITDA Sagrado de Telecom
    private BigDecimal ebitda;
    private BigDecimal ebitdaMarginPercentage;

    // 8. Investimentos e Amortizações (CAPEX)
    private BigDecimal capexAmortization;

    // 9. Fluxo de Caixa Livre Operacional
    private BigDecimal freeCashFlow;
}
