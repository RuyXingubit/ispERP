package br.dev.xb.isperp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RadiusDisconnectRequest {
    @NotBlank(message = "O nome de usuário PPPoE é obrigatório")
    private String username;

    private @Nullable String nasIpAddress;
    private @Nullable String acctSessionId;
    private @Nullable String framedIpAddress;
}
