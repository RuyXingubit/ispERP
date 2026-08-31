package br.dev.xb.isperp.dto;

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
public class OltPonPortResponse {
    private UUID id;
    private @Nullable UUID companyId;
    private UUID networkDeviceId;
    private @Nullable String oltName;
    private int slotNumber;
    private int portNumber;
    private String ponName;
    private String adminStatus;
    private String operStatus;
    private BigDecimal txPowerDbm;
    private BigDecimal temperatureCelsius;
    private int totalOnus;
    private int onlineOnus;
    private int losOnus;
    private int dyingGaspOnus;
    private int offlineOnus;
    private double healthPercentage;
    private @Nullable UUID connectedCableId;
    private @Nullable String connectedCableName;
    private @Nullable OffsetDateTime lastPolledAt;
    private OffsetDateTime createdAt;
}
