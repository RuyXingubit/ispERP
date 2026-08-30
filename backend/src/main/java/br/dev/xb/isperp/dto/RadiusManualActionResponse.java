package br.dev.xb.isperp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RadiusManualActionResponse {

    private UUID contractId;
    private String username;
    private String actionApplied;
    private boolean success;
    private String message;
    private @Nullable String podResult;
}
