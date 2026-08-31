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
public class FtthPopResponse {
    private UUID id;
    private @Nullable UUID companyId;
    private String name;
    private @Nullable BigDecimal latitude;
    private @Nullable BigDecimal longitude;
    private @Nullable String address;
    private @Nullable String description;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
