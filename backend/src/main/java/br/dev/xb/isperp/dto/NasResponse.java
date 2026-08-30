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
public class NasResponse {
    private UUID id;
    private @Nullable UUID companyId;
    private String nasname;
    private @Nullable String shortname;
    private String type;
    private @Nullable Integer ports;
    private String secret;
    private @Nullable String server;
    private @Nullable String community;
    private @Nullable String description;
    private NasVendorType vendorType;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
