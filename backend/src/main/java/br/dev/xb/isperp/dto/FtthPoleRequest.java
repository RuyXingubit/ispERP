package br.dev.xb.isperp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FtthPoleRequest {
    private @Nullable UUID companyId;

    @NotBlank(message = "O código do poste é obrigatório")
    private String code;

    @NotNull(message = "A latitude é obrigatória")
    private BigDecimal latitude;

    @NotNull(message = "A longitude é obrigatória")
    private BigDecimal longitude;

    @Builder.Default
    private String poleType = "CONCRETO";

    @Builder.Default
    private int reservationMeters = 0;

    private @Nullable String description;
}
