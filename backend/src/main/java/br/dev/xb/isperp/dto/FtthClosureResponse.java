package br.dev.xb.isperp.dto;

import br.dev.xb.isperp.ftth.FtthClosureType;
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
public class FtthClosureResponse {
    private UUID id;
    private @Nullable UUID companyId;
    private String name;
    private @Nullable UUID poleId;
    private @Nullable String poleCode;
    private @Nullable BigDecimal latitude;
    private @Nullable BigDecimal longitude;
    private FtthClosureType closureType;
    private int trayCount;
    private int capacityFusions;
    private int usedFusionsCount;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
