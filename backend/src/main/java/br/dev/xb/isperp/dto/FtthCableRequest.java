package br.dev.xb.isperp.dto;

import br.dev.xb.isperp.ftth.FiberColorStandard;
import br.dev.xb.isperp.ftth.FtthCableType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FtthCableRequest {
    private @Nullable UUID companyId;

    @NotBlank(message = "O nome do cabo é obrigatório")
    private String name;

    @Builder.Default
    private FtthCableType cableType = FtthCableType.DISTRIBUICAO;

    @Min(value = 1, message = "O número de fibras deve ser no mínimo 1")
    @Builder.Default
    private int fiberCount = 12;

    @Min(value = 1, message = "O número de tubos deve ser no mínimo 1")
    @Builder.Default
    private int tubeCount = 1;

    @Builder.Default
    private FiberColorStandard colorStandard = FiberColorStandard.ABNT_NBR_14106;

    @Builder.Default
    private BigDecimal lengthMeters = BigDecimal.ZERO;

    private @Nullable String pathCoordinates; // GeoJSON
    private @Nullable UUID sourcePopId;
    private @Nullable UUID sourcePoleId;
    private @Nullable UUID targetPoleId;

    @Builder.Default
    private BigDecimal attenuationDbPerKm = new BigDecimal("0.35");
}
