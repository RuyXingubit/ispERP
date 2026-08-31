package br.dev.xb.isperp.dto;

import br.dev.xb.isperp.monitoring.IncidentSeverity;
import br.dev.xb.isperp.monitoring.IncidentStatus;
import br.dev.xb.isperp.monitoring.IncidentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FtthIncidentResponse {
    private UUID id;
    private @Nullable UUID companyId;
    private @Nullable UUID networkDeviceId;
    private @Nullable String oltName;
    private @Nullable UUID oltPonPortId;
    private @Nullable String ponName;
    private IncidentType incidentType;
    private String incidentTypeDescription;
    private IncidentSeverity severity;
    private IncidentStatus status;
    private String title;
    private @Nullable String description;
    private int affectedCustomersCount;
    private @Nullable String affectedCtosIds;
    private @Nullable List<String> affectedCtoNames;
    private @Nullable UUID affectedCableId;
    private @Nullable String affectedCableName;
    private @Nullable BigDecimal estimatedCutLatitude;
    private @Nullable BigDecimal estimatedCutLongitude;
    private @Nullable String estimatedCutDetails;
    private @Nullable UUID workOrderId;
    private @Nullable String workOrderProtocol;
    private OffsetDateTime detectedAt;
    private @Nullable OffsetDateTime dispatchedAt;
    private @Nullable OffsetDateTime resolvedAt;
    private @Nullable String rootCauseNotes;
}
