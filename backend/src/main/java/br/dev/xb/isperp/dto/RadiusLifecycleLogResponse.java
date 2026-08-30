package br.dev.xb.isperp.dto;

import br.dev.xb.isperp.radius.RadiusLifecycleActionType;
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
public class RadiusLifecycleLogResponse {

    private UUID id;
    private UUID contractId;
    private UUID customerId;
    private @Nullable String customerName;
    private String username;
    private RadiusLifecycleActionType actionType;
    private @Nullable String reason;
    private @Nullable String nasIp;
    private boolean success;
    private @Nullable String details;
    private OffsetDateTime createdAt;
}
