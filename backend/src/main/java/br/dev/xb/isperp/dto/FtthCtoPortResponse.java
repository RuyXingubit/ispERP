package br.dev.xb.isperp.dto;

import br.dev.xb.isperp.ftth.FtthPortStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FtthCtoPortResponse {
    private UUID id;
    private UUID ctoId;
    private int portNumber;
    private FtthPortStatus status;
    private @Nullable UUID onuProvisioningId;
    private @Nullable String onuSerial;
    private @Nullable String onuMac;
    private @Nullable UUID customerId;
    private @Nullable String customerName;
    private @Nullable String pppoeUser;
    private @Nullable String notes;
    private OffsetDateTime createdAt;
}
