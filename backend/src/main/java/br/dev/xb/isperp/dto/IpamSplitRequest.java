package br.dev.xb.isperp.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IpamSplitRequest {
    @NotNull(message = "O ID da sub-rede pai é obrigatório")
    private UUID subnetId;

    @NotNull(message = "O prefixo alvo é obrigatório (ex: 28 para dividir /24 em blocos /28)")
    private Integer targetPrefixLength;

    @Builder.Default
    private boolean createSubnets = false; // Se true, já persiste as sub-redes filhas no banco
}
