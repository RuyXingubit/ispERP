package br.dev.xb.isperp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicValidationResponse {
    private boolean valid;
    private String validationToken;
    private String sha256Hash;
    private @Nullable String courtOrderNumber;
    private @Nullable String requesterAuthority;
    private String queriedIp;
    private @Nullable Integer queriedPort;
    private OffsetDateTime queriedTimestamp;
    private @Nullable String customerNameMasked;
    private @Nullable String customerCpfCnpjMasked;
    private @Nullable String callingStationId;
    private OffsetDateTime reportIssuedAt;
    private String statusMessage;
}
