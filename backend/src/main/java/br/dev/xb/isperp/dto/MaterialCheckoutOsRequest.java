package br.dev.xb.isperp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaterialCheckoutOsRequest {
    private UUID workOrderId;
    private UUID technicianUserId;
    private @Nullable UUID warehouseId;
    private @Nullable String itemCode;
    private @Nullable UUID assetId;
    private int quantityOrMeters;
    private @Nullable String beforePhotoUrl;
    private @Nullable String notes;
}
