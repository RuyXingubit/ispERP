package br.dev.xb.isperp.dto;

import br.dev.xb.isperp.ipam.IpamAddressStatus;
import br.dev.xb.isperp.ipam.IpamAssignedToType;
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
public class IpamIpAddressResponse {
    private UUID id;
    private UUID subnetId;
    private String subnetCidr;
    private String ipAddress;
    private IpamAddressStatus status;
    private @Nullable IpamAssignedToType assignedToType;
    private @Nullable UUID assignedToId;
    private @Nullable String assignedToLabel;
    private @Nullable String dnsName;
    private @Nullable String description;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
