package br.dev.xb.isperp.dto;

import br.dev.xb.isperp.ipam.SubnetCalculationResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IpamSplitResponse {
    private UUID parentSubnetId;
    private String parentCidr;
    private int targetPrefixLength;
    private int totalSubnetsGenerated;
    private List<SubnetCalculationResult> generatedSubnets;
    private List<IpamSubnetResponse> persistedSubnets;
}
