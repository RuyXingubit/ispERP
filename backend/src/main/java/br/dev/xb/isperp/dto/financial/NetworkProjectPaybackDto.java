package br.dev.xb.isperp.dto.financial;

import br.dev.xb.isperp.entity.financial.ProjectStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NetworkProjectPaybackDto {
    private UUID projectId;
    private String name;
    private String neighborhood;
    private String city;
    private ProjectStatus status;
    private BigDecimal budgetAmount;
    private Integer targetSubscribers;
    private LocalDate startDate;

    // Métricas Físicas de Rede
    private Integer ctoCount;
    private Integer totalPorts;
    private Integer occupiedPorts;
    private BigDecimal occupancyRatePercentage;

    // Métricas Financeiras & Payback
    private Integer activeSubscribers;
    private BigDecimal generatedMrr;
    private BigDecimal monthlyNetContribution;
    private BigDecimal accumulatedPaybackMonths;
    private Boolean isPaybackReached;

    // O Direcionador Comercial (Termômetro do Dono)
    private String commercialDirectionAlert;
    private String priorityLevel; // HIGH_RETURN, IDLE_NETWORK_FOCUS, CRITICAL_UNDERPERFORMING
}
