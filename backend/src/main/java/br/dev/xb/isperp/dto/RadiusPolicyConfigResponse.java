package br.dev.xb.isperp.dto;

import br.dev.xb.isperp.radius.RadiusBlockMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RadiusPolicyConfigResponse {

    private UUID id;
    private boolean autoBlockEnabled;
    private int toleranceDays;
    private RadiusBlockMode blockMode;
    private int reducedDownloadKbps;
    private int reducedUploadKbps;
    private boolean unblockOnPayment;
    private boolean sendPodOnBlock;
    private boolean sendPodOnUnblock;
    private OffsetDateTime createdAt;
    private @Nullable OffsetDateTime updatedAt;
}
