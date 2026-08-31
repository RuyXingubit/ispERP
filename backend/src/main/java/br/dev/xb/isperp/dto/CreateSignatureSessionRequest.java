package br.dev.xb.isperp.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSignatureSessionRequest {

    @NotNull(message = "ID do contrato é obrigatório")
    private UUID contractId;

    private @Nullable UUID templateId;

    private @Nullable BigDecimal symbolicAmount;
}
