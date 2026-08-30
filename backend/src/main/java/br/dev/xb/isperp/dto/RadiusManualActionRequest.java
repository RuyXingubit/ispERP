package br.dev.xb.isperp.dto;

import br.dev.xb.isperp.radius.NasVendorType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RadiusManualActionRequest {

    @NotNull(message = "O ID do contrato é obrigatório")
    private UUID contractId;

    @NotBlank(message = "A ação (BLOCK / UNBLOCK) é obrigatória")
    private String action;

    private @Nullable String reason;

    private @Nullable NasVendorType vendorType;

    private @Nullable Boolean sendPod;
}
