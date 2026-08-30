package br.dev.xb.isperp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SuppressWarnings("null")
public class FiscalRegimeTransitionRequest {

    @Nullable
    private UUID companyId;

    @NotBlank(message = "O novo regime tributário é obrigatório")
    private String newRegime;

    @NotNull(message = "A data de vigência é obrigatória")
    private LocalDate effectiveDate;

    @Builder.Default
    private BigDecimal aliquotaIcms = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal aliquotaPis = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal aliquotaCofins = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal aliquotaFust = new BigDecimal("0.65");

    @Builder.Default
    private BigDecimal aliquotaFunttel = new BigDecimal("0.50");

    @Nullable
    private String notes;
}
