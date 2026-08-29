package br.dev.xb.isperp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutToolRequest {

    private UUID workOrderId;

    private UUID holderUserId;

    @NotBlank(message = "Nome do responsável é obrigatório")
    private String holderName;

    @NotBlank(message = "CPF do responsável é obrigatório")
    private String holderCpf;

    private Boolean isThirdParty;

    @NotNull(message = "Lista de equipamentos é obrigatória")
    private List<UUID> assetIds; // Máquina de Fusão, OTDR, etc.

    private BigDecimal totalPromissoryValue;

    private String dispatchPhotoUrl;

    private String notes;
}
