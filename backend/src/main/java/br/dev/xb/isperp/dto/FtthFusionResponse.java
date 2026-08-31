package br.dev.xb.isperp.dto;

import br.dev.xb.isperp.ftth.FiberColorInfo;
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
public class FtthFusionResponse {
    private UUID id;
    private UUID closureId;
    private int trayNumber;
    private UUID sourceCableId;
    private String sourceCableName;
    private int sourceFiberNumber;
    private @Nullable FiberColorInfo sourceFiberColor;
    private @Nullable UUID targetCableId;
    private @Nullable String targetCableName;
    private @Nullable Integer targetFiberNumber;
    private @Nullable FiberColorInfo targetFiberColor;
    private @Nullable UUID targetSplitterId;
    private @Nullable String targetSplitterName;
    private @Nullable UUID targetCtoId;
    private @Nullable String targetCtoName;
    private BigDecimal lossDb;
    private @Nullable String description;
    private OffsetDateTime createdAt;
}
