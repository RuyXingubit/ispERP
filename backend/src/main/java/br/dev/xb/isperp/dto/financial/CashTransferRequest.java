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
public class CashTransferRequest {

    @NotNull(message = "ID do usuário recebedor é obrigatório")
    private UUID receiverUserId;

    @NotNull(message = "Valor a transferir é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
    private BigDecimal amount;

    private String reason;
    private String notes;
}
