package br.dev.xb.isperp.dto;

import br.dev.xb.isperp.radius.NasVendorType;
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
public class CgnatMappingRequest {
    private @Nullable UUID nasId;

    @Builder.Default
    private NasVendorType vendorType = NasVendorType.MIKROTIK;

    @NotBlank(message = "O IP público é obrigatório")
    private String publicIp;

    @NotNull(message = "A porta inicial é obrigatória")
    private Integer portStart;

    @NotNull(message = "A porta final é obrigatória")
    private Integer portEnd;

    @NotBlank(message = "O IP privado inicial é obrigatório")
    private String privateIpStart;

    @NotBlank(message = "O IP privado final é obrigatório")
    private String privateIpEnd;

    @Builder.Default
    private String protocol = "BOTH";

    private @Nullable String notes;
}
