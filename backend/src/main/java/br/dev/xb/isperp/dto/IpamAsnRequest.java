package br.dev.xb.isperp.dto;

import br.dev.xb.isperp.ipam.IpamRir;
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
public class IpamAsnRequest {
    private @Nullable UUID companyId;

    @NotNull(message = "O número do ASN é obrigatório")
    private Long asn;

    @NotBlank(message = "O nome do titular/ASN é obrigatório")
    private String name;

    @Builder.Default
    private IpamRir rir = IpamRir.REGISTRO_BR;

    private @Nullable String description;
}
