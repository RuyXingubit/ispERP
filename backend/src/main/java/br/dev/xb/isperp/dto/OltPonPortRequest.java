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
public class OltPonPortRequest {
    private @Nullable UUID companyId;

    @NotNull(message = "A OLT é obrigatória")
    private UUID networkDeviceId;

    @Builder.Default
    private int slotNumber = 0;

    @Builder.Default
    private int portNumber = 1;

    @NotBlank(message = "O nome da PON é obrigatório")
    private String ponName;

    private @Nullable UUID connectedCableId;
    private @Nullable BigDecimal txPowerDbm;
}
