package br.dev.xb.isperp.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FtthFusionRequest {

    @NotNull(message = "A caixa de emenda é obrigatória")
    private UUID closureId;

    @Builder.Default
    @Min(value = 1)
    private int trayNumber = 1;

    @NotNull(message = "O cabo de origem é obrigatório")
    private UUID sourceCableId;

    @Min(value = 1, message = "O número da fibra de origem deve ser no mínimo 1")
    private int sourceFiberNumber;

    // Destino A: Outra Fibra
    private @Nullable UUID targetCableId;
    private @Nullable Integer targetFiberNumber;

    // Destino B: Splitter
    private @Nullable UUID targetSplitterId;

    // Destino C: Alimentação de CTO
    private @Nullable UUID targetCtoId;

    @Builder.Default
    private BigDecimal lossDb = new BigDecimal("0.05");

    private @Nullable String description;
}
