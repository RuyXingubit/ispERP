package br.dev.xb.isperp.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FtthFeasibilityRequest {
    @NotNull(message = "A latitude é obrigatória")
    private BigDecimal latitude;

    @NotNull(message = "A longitude é obrigatória")
    private BigDecimal longitude;

    @Builder.Default
    private double maxDistanceMeters = 200.0;
}
