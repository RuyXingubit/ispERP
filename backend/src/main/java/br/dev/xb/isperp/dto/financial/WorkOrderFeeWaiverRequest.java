package br.dev.xb.isperp.dto.financial;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkOrderFeeWaiverRequest {

    @NotNull(message = "ID da Ordem de Serviço é obrigatório")
    private UUID workOrderId;

    @NotBlank(message = "Justificativa comercial para isenção é obrigatória")
    private String waiverReason;
}
