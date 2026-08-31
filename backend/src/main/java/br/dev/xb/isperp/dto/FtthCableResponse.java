package br.dev.xb.isperp.dto;

import br.dev.xb.isperp.ftth.FiberColorInfo;
import br.dev.xb.isperp.ftth.FiberColorStandard;
import br.dev.xb.isperp.ftth.FtthCableType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FtthCableResponse {
    private UUID id;
    private @Nullable UUID companyId;
    private String name;
    private FtthCableType cableType;
    private int fiberCount;
    private int tubeCount;
    private FiberColorStandard colorStandard;
    private BigDecimal lengthMeters;
    private @Nullable String pathCoordinates;
    private @Nullable UUID sourcePopId;
    private @Nullable UUID sourcePoleId;
    private @Nullable UUID targetPoleId;
    private BigDecimal attenuationDbPerKm;
    private @Nullable List<FiberColorInfo> fibers;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
