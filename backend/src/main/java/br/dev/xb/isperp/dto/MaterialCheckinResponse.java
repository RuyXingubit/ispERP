package br.dev.xb.isperp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaterialCheckinResponse {
    private UUID logId;
    private UUID workOrderId;
    private UUID technicianUserId;
    private String status; // CONFORMANT, DIVERGENT
    private boolean hasDivergence;
    private int expectedRemaining;
    private int actualRemaining;
    private int divergenceQuantity;
    private @Nullable String beforePhotoUrl;
    private @Nullable String installedPhotoUrl;
    private @Nullable String returnPhotoUrl;
    private @Nullable String notes;
    private LocalDateTime checkedAt;
}
