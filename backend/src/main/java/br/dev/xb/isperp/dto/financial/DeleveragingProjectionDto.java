package br.dev.xb.isperp.dto.financial;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeleveragingProjectionDto {
    // 1. Números Sagrados do Dono
    private BigDecimal currentEbitda;
    private BigDecimal currentMrr;
    private BigDecimal monthlyChurnRatePercentage;
    private BigDecimal startingCashBalance;

    // 2. Fundo do Poço (Maximum Drawdown)
    private String worstMonthYearMonth; // "2027-02"
    private BigDecimal worstMonthProjectedBalance;

    // 3. Data da Alforria / Virada de Caixa
    private String breakEvenYearMonth; // "2027-11"
    private Integer monthsUntilFreedom;

    // 4. Curva Completa de 36 Meses
    @Builder.Default
    private List<MonthlyProjectionPointDto> timeline = new ArrayList<>();
}
