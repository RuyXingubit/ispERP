package br.dev.xb.isperp.dto;

import br.dev.xb.isperp.ftth.FtthSplitterType;
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
public class FtthSplitterResponse {
    private UUID id;
    private @Nullable UUID companyId;
    private @Nullable UUID closureId;
    private String name;
    private FtthSplitterType splitterType;
    private @Nullable UUID inputCableId;
    private @Nullable Integer inputFiberNumber;
    private BigDecimal attenuationDb;
    private int outputPorts;
    private OffsetDateTime createdAt;
}
