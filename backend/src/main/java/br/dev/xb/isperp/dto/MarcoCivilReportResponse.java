package br.dev.xb.isperp.dto;

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
public class MarcoCivilReportResponse {
    private UUID id;
    private String validationToken;
    private String sha256Hash;
    private String publicValidationUrl;
    private String qrCodePayload;
    private @Nullable String courtOrderNumber;
    private @Nullable String requesterAuthority;
    private String queriedIp;
    private @Nullable Integer queriedPort;
    private OffsetDateTime queriedTimestamp;
    private @Nullable UUID matchedContractId;
    private @Nullable String matchedCustomerName;
    private @Nullable String matchedCpfCnpj;
    private @Nullable String matchedCallingStationId;
    private @Nullable OffsetDateTime matchedSessionStart;
    private @Nullable OffsetDateTime matchedSessionStop;
    private @Nullable String reportPdfUrl;
    private @Nullable String notes;
    private OffsetDateTime createdAt;
}
