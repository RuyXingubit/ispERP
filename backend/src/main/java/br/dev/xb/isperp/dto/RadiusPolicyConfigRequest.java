package br.dev.xb.isperp.dto;

import br.dev.xb.isperp.radius.RadiusBlockMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RadiusPolicyConfigRequest {

    private boolean autoBlockEnabled;

    @Min(value = 1, message = "Os dias de tolerância devem ser no mínimo 1 dia")
    private int toleranceDays;

    @NotNull(message = "O modo de bloqueio é obrigatório")
    private RadiusBlockMode blockMode;

    private int reducedDownloadKbps;
    private int reducedUploadKbps;

    private boolean unblockOnPayment;
    private boolean sendPodOnBlock;
    private boolean sendPodOnUnblock;

    @Min(value = 0)
    @Max(value = 23)
    private int blockStartHour;

    @Min(value = 0)
    @Max(value = 23)
    private int blockEndHour;

    private boolean allowBlockOnFriday;
    private boolean protectEveOfHolidays;
}
