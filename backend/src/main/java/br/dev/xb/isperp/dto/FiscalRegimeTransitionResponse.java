package br.dev.xb.isperp.dto;

import br.dev.xb.isperp.fiscal.FiscalRegimeTransitionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SuppressWarnings("null")
public class FiscalRegimeTransitionResponse {

    @Nullable
    private UUID id;

    private UUID companyId;

    private String previousRegime;

    private String newRegime;

    private LocalDate effectiveDate;

    private BigDecimal aliquotaIcms;

    private BigDecimal aliquotaPis;

    private BigDecimal aliquotaCofins;

    private BigDecimal aliquotaFust;

    private BigDecimal aliquotaFunttel;

    private FiscalRegimeTransitionStatus status;

    @Nullable
    private String notes;

    @Nullable
    private LocalDateTime appliedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
