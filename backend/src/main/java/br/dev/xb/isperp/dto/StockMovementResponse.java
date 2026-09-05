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
public class StockMovementResponse {
    private UUID id;
    private LocalDateTime createdAt;
    private String eventType;
    private String eventDescription;
    private Integer quantity;
    private Integer balanceAfter;
    private @Nullable String warehouseName;
    private @Nullable UUID warehouseId;
    private @Nullable String userName;
    private @Nullable UUID userId;
    private @Nullable UUID workOrderId;
    private @Nullable String notes;
    private @Nullable String photoUrl;
}
