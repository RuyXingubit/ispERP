package br.dev.xb.isperp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NocMonitoringSummaryResponse {
    private int totalOlts;
    private int totalPonPorts;
    private int activePonPorts;
    private int totalOnus;
    private int onlineOnus;
    private int losOnus;
    private int dyingGaspOnus;
    private int offlineOnus;
    private double globalHealthPercentage;
    private int activeIncidentsCount;
    private int criticalIncidentsCount;
    private List<FtthIncidentResponse> activeIncidents;
}
