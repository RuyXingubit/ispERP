package br.dev.xb.isperp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FtthCtoRequest {
    private @Nullable UUID companyId;

    @NotBlank(message = "O nome da CTO é obrigatório")
    private String name;

    private @Nullable UUID poleId;
    private @Nullable UUID closureId;

    @NotNull(message = "A latitude da CTO é obrigatória")
    private BigDecimal latitude;

    @NotNull(message = "A longitude da CTO é obrigatória")
    private BigDecimal longitude;

    @Builder.Default
    private int totalPorts = 16;

    @Builder.Default
    private String splitterType = "BALANCED_1_16";

    @Builder.Default
    private String status = "ATIVA";

    private @Nullable String description;
}
