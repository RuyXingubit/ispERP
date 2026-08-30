package br.dev.xb.isperp.dto;

import br.dev.xb.isperp.radius.NasVendorType;
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
public class CgnatMappingResponse {
    private UUID id;
    private @Nullable UUID nasId;
    private @Nullable String nasName;
    private NasVendorType vendorType;
    private String publicIp;
    private Integer portStart;
    private Integer portEnd;
    private String privateIpStart;
    private String privateIpEnd;
    private String protocol;
    private @Nullable String notes;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
