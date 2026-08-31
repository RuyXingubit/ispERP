package br.dev.xb.isperp.dto;

import br.dev.xb.isperp.entity.MaterialDemandStatus;
import lombok.*;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstallationMaterialDemandResponse {

    private UUID id;
    private UUID workOrderId;
    private UUID contractId;
    private @Nullable String contractNumber;
    private @Nullable String customerName;
    private @Nullable String customerPhone;
    private @Nullable String customerAddress;
    private @Nullable BigDecimal customerLatitude;
    private @Nullable BigDecimal customerLongitude;
    private @Nullable UUID ctoId;
    private @Nullable String ctoName;
    private @Nullable BigDecimal ctoLatitude;
    private @Nullable BigDecimal ctoLongitude;
    private @Nullable Integer ctoPortNumber;
    private Integer estimatedDropMeters;
    private String onuModelRequired;
    private Integer fastConnectorsCount;
    private Integer ptoRosetteCount;
    private MaterialDemandStatus status;
    private @Nullable UUID allocatedWarehouseId;
    private @Nullable String allocatedWarehouseName;
    private @Nullable String allocatedTechnicianName;
    private OffsetDateTime createdAt;
}
