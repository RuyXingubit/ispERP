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
public class MarcoCivilSearchResult {
    private boolean matched;
    private String queriedIp;
    private @Nullable Integer queriedPort;
    private OffsetDateTime queriedTimestamp;

    // Resolução CGNAT
    private boolean usedCgnat;
    private @Nullable String resolvedPrivateIp;
    private @Nullable String cgnatRuleSummary;

    // Sessão RADIUS
    private @Nullable Long radacctId;
    private @Nullable String username;
    private @Nullable String callingStationId; // MAC ONT
    private @Nullable String nasIpAddress;
    private @Nullable OffsetDateTime sessionStartTime;
    private @Nullable OffsetDateTime sessionStopTime;

    // Dados do Assinante Identificado
    private @Nullable UUID contractId;
    private @Nullable String contractNumber;
    private @Nullable UUID customerId;
    private @Nullable String customerName;
    private @Nullable String customerCpfCnpj;
    private @Nullable String customerPhone;
    private @Nullable String customerEmail;
    private @Nullable String installationAddress;
    private @Nullable String planName;
}
