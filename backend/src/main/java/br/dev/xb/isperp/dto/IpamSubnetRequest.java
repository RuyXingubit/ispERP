package br.dev.xb.isperp.dto;

import br.dev.xb.isperp.ipam.IpamSubnetCategory;
import br.dev.xb.isperp.ipam.IpamSubnetStatus;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IpamSubnetRequest {
    private @Nullable UUID parentId;
    private @Nullable UUID vrfId;
    private @Nullable UUID asnId;
    private @Nullable UUID companyId;

    @NotBlank(message = "O CIDR da sub-rede é obrigatório (ex: 200.150.10.0/24)")
    private String cidr;

    @JsonProperty("isPool")
    @JsonAlias({"pool", "isPool"})
    @Builder.Default
    private boolean isPool = false;

    private @Nullable String poolName;

    @Builder.Default
    private IpamSubnetStatus status = IpamSubnetStatus.ACTIVE;

    @Builder.Default
    private IpamSubnetCategory category = IpamSubnetCategory.CUSTOMER_ACCESS;

    private @Nullable String description;
}
