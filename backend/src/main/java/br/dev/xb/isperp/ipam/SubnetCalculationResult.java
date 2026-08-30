package br.dev.xb.isperp.ipam;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubnetCalculationResult {
    private String cidr;
    private IpamIpVersion ipVersion;
    private String networkAddress;
    private @Nullable String broadcastAddress;
    private String netmask;
    private @Nullable String wildcardMask;
    private String firstUsableIp;
    private String lastUsableIp;
    private int prefixLength;
    private long totalHosts;
    private long usableHosts;
}
