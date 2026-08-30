package br.dev.xb.isperp.dto;

import br.dev.xb.isperp.ipam.IpamAddressStatus;
import br.dev.xb.isperp.ipam.IpamAssignedToType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class IpamIpAddressRequest {
    @NotNull(message = "O ID da sub-rede é obrigatório")
    private UUID subnetId;

    @NotBlank(message = "O endereço IP é obrigatório")
    private String ipAddress;

    @Builder.Default
    private IpamAddressStatus status = IpamAddressStatus.AVAILABLE;

    private @Nullable IpamAssignedToType assignedToType;
    private @Nullable UUID assignedToId;
    private @Nullable String dnsName;
    private @Nullable String description;
}
