package br.dev.xb.isperp.dto;

import br.dev.xb.isperp.ipam.IpamIpVersion;
import br.dev.xb.isperp.ipam.IpamSubnetCategory;
import br.dev.xb.isperp.ipam.IpamSubnetStatus;
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
public class IpamSubnetResponse {
    private UUID id;
    private @Nullable UUID parentId;
    private @Nullable UUID vrfId;
    private @Nullable String vrfName;
    private @Nullable UUID asnId;
    private @Nullable Long asnNumber;
    private @Nullable UUID companyId;
    private String cidr;
    private IpamIpVersion ipVersion;
    private String networkAddress;
    private @Nullable String broadcastAddress;
    private Integer prefixLength;
    private Long totalHosts;
    private Long allocatedHosts;
    private Double utilizationPercentage;
    private boolean isPool;
    private @Nullable String poolName;
    private IpamSubnetStatus status;
    private IpamSubnetCategory category;
    private @Nullable String description;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
