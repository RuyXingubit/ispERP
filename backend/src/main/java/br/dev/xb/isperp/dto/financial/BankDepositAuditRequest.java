package br.dev.xb.isperp.dto.financial;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankDepositAuditRequest {

    @NotNull(message = "Decisão de auditoria é obrigatória")
    private Boolean approved;

    private String notes;
    private String rejectionReason;
}
