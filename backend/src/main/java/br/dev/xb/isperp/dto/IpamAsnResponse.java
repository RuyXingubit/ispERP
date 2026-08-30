package br.dev.xb.isperp.dto;

import br.dev.xb.isperp.ipam.IpamRir;
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
public class IpamAsnResponse {
    private UUID id;
    private @Nullable UUID companyId;
    private Long asn;
    private String name;
    private IpamRir rir;
    private @Nullable String description;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
