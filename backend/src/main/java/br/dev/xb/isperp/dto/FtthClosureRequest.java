package br.dev.xb.isperp.dto;

import br.dev.xb.isperp.ftth.FtthClosureType;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FtthClosureRequest {
    private @Nullable UUID companyId;

    @NotBlank(message = "O nome da caixa de emenda é obrigatório")
    private String name;

    private @Nullable UUID poleId;
    private @Nullable BigDecimal latitude;
    private @Nullable BigDecimal longitude;

    @Builder.Default
    private FtthClosureType closureType = FtthClosureType.DOMO;

    @Builder.Default
    private int trayCount = 4;

    @Builder.Default
    private int capacityFusions = 48;

    @Builder.Default
    private String status = "ATIVA";
}
