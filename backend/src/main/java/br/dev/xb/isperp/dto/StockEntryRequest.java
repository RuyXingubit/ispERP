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
public class StockEntryRequest {
    private UUID warehouseId;
    private String itemCode;
    private String itemName;
    private String category;
    private int quantity;
    private String unit;
    private @Nullable String notes;
}
