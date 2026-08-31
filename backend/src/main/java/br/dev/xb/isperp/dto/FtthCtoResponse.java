package br.dev.xb.isperp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FtthCtoResponse {
    private UUID id;
    private @Nullable UUID companyId;
    private String name;
    private @Nullable UUID poleId;
    private @Nullable String poleCode;
    private @Nullable UUID closureId;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private int totalPorts;
    private int freePortsCount;
    private int occupiedPortsCount;
    private double occupancyPercentage;
    private String splitterType;
    private String status;
    private @Nullable String description;
    private @Nullable List<FtthCtoPortResponse> ports;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
