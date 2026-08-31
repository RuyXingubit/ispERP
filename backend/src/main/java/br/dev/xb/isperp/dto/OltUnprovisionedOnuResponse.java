package br.dev.xb.isperp.dto;

import lombok.*;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OltUnprovisionedOnuResponse {

    private UUID networkDeviceId;
    private String oltName;
    private Integer slotNumber;
    private Integer portNumber;
    private String ponName;
    private String onuSerial;
    private @Nullable String onuMac;
    private @Nullable BigDecimal rxPowerDbm;
    private OffsetDateTime detectedAt;
}
