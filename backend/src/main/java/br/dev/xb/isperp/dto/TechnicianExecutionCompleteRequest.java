package br.dev.xb.isperp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TechnicianExecutionCompleteRequest {

    @NotBlank(message = "Serial da ONU é obrigatório")
    private String onuSerial;

    private @Nullable String onuMac;

    private @Nullable Integer vlanId;

    private @Nullable String pppoeUsername;

    private @Nullable String pppoePassword;

    private @Nullable String wifiSsid;

    private @Nullable String wifiPassword;

    private @Nullable BigDecimal fiberSignalDbm;

    private @Nullable String installationPhotoUrl;

    private @Nullable String digitalSignatureBase64;

    private @Nullable String customerSignatureName;

    private @Nullable String notes;

    private @Nullable BigDecimal technicianLatitude;

    private @Nullable BigDecimal technicianLongitude;

    private @Nullable UUID warehouseId;
}
