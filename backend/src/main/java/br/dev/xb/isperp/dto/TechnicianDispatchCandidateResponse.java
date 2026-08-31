package br.dev.xb.isperp.dto;

import lombok.*;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TechnicianDispatchCandidateResponse {

    private UUID technicianId;
    private String technicianName;
    private @Nullable UUID warehouseId;
    private @Nullable String vehicleWarehouseName;
    private Boolean hasCompleteKit;
    private Boolean hasOnu;
    private Boolean hasDropCable;
    private Boolean hasConnectors;
    private Integer dropCableBalanceMeters;
    private @Nullable BigDecimal currentLatitude;
    private @Nullable BigDecimal currentLongitude;
    private @Nullable Double distanceKmToCustomer;
    private @Nullable String lastServiceAddress;
    private Double recommendedScore; // Pontuação combinada (Estoque + Proximidade)
}
