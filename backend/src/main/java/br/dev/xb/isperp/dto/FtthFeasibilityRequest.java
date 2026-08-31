package br.dev.xb.isperp.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

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
