package br.dev.xb.isperp.dto;

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
public class FtthPoleResponse {
    private UUID id;
    private @Nullable UUID companyId;
    private String code;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String poleType;
    private int reservationMeters;
    private @Nullable String description;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
