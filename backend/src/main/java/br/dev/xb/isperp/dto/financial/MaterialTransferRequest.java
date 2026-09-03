package br.dev.xb.isperp.dto.financial;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaterialTransferRequest {

    @NotNull(message = "ID do técnico recebedor é obrigatório")
    private UUID receiverUserId;

    @NotNull(message = "ID da custódia do material é obrigatório")
    private UUID materialCustodyId;

    @NotNull(message = "Quantidade a transferir é obrigatória")
    @DecimalMin(value = "0.01", message = "Quantidade deve ser maior que zero")
    private BigDecimal quantity;

    private String notes;
}
