package br.dev.xb.isperp.dto;

import br.dev.xb.isperp.radius.NasVendorType;
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
public class NasRequest {
    private @Nullable UUID companyId;

    @NotBlank(message = "O endereço IP / FQDN do NAS/BNG é obrigatório")
    private String nasname;

    private @Nullable String shortname;

    @Builder.Default
    private String type = "other";

    private @Nullable Integer ports;

    @NotBlank(message = "O Secret compartilhado RADIUS é obrigatório")
    private String secret;

    private @Nullable String server;
    private @Nullable String community;
    private @Nullable String description;

    @Builder.Default
    private NasVendorType vendorType = NasVendorType.MIKROTIK;
}
