package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.*;
import br.dev.xb.isperp.entity.*;
import br.dev.xb.isperp.mapper.FtthMonitoringMapper;
import br.dev.xb.isperp.monitoring.IncidentSeverity;
import br.dev.xb.isperp.monitoring.IncidentStatus;
import br.dev.xb.isperp.monitoring.IncidentType;
import br.dev.xb.isperp.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class FtthCorrelationEngine {

    private final FtthIncidentRepository incidentRepository;
    private final OltPonPortRepository oltPonPortRepository;
    private final NetworkDeviceRepository networkDeviceRepository;
    private final OnuProvisioningRepository onuProvisioningRepository;
    private final FtthCtoRepository ctoRepository;
    private final FtthCtoPortRepository ctoPortRepository;
    private final FtthCableRepository cableRepository;
    private final FtthMonitoringMapper mapper;
    private final ObjectMapper objectMapper;

    /**
     * Analisa todas as portas PON e CTOs para correlacionar falhas em massa.
     */
    @Transactional
    public List<FtthIncidentResponse> runCorrelationAnalysis() {
        List<FtthIncident> createdIncidents = new ArrayList<>();
        List<OltPonPort> ponPorts = oltPonPortRepository.findAll();

        for (OltPonPort pon : ponPorts) {
            // Se houver ONUs com LOS nessa PON
            if (pon.getLosOnus() > 0) {
                // Analisa agrupamento por CTOs
                List<OnuProvisioning> onus = onuProvisioningRepository.findByNetworkDeviceId(pon.getNetworkDeviceId());
                Map<UUID, List<OnuProvisioning>> ctoOnusMap = new HashMap<>();

                for (OnuProvisioning onu : onus) {
                    ctoPortRepository.findByOnuProvisioningId(onu.getId()).ifPresent(port -> {
                        ctoOnusMap.computeIfAbsent(port.getCtoId(), k -> new ArrayList<>()).add(onu);
                    });
                }

                List<UUID> affectedCtos = new ArrayList<>();
                int totalAffectedCustomers = 0;

                for (Map.Entry<UUID, List<OnuProvisioning>> entry : ctoOnusMap.entrySet()) {
                    UUID ctoId = entry.getKey();
                    List<OnuProvisioning> ctoOnus = entry.getValue();
                    long ctoLosCount = ctoOnus.stream()
                            .filter(o -> o.getRxPowerDbm() != null && o.getRxPowerDbm().compareTo(new BigDecimal("-40.00")) <= 0)
                            .count();

                    // Se mais de 60% das ONUs da CTO caíram com LOS, a CTO inteira é considerada offline
                    if (ctoLosCount >= 2 && ((double) ctoLosCount / ctoOnus.size()) >= 0.6) {
                        affectedCtos.add(ctoId);
                        totalAffectedCustomers += (int) ctoLosCount;
                    }
                }

                // Se identificou CTOs afetadas em massa
                if (!affectedCtos.isEmpty()) {
                    Optional<FtthIncident> activeIncident = incidentRepository.findAll().stream()
                            .filter(i -> i.getStatus() != IncidentStatus.RESOLVED && pon.getId().equals(i.getOltPonPortId()))
                            .findFirst();

                    if (activeIncident.isEmpty()) {
                        FtthIncident incident = createIncidentForPon(pon, affectedCtos, totalAffectedCustomers);
                        createdIncidents.add(incident);
                    }
                }
            }
        }

        return createdIncidents.stream().map(this::enrichIncidentResponse).toList();
    }

    private FtthIncident createIncidentForPon(OltPonPort pon, List<UUID> affectedCtos, int totalAffectedCustomers) {
        IncidentType type;
        IncidentSeverity severity;
        String title;

        // Se mais de 1 CTO caiu na mesma PON ➔ Rompimento no Cabo Troncal / Alimentador
        if (affectedCtos.size() > 1) {
            type = IncidentType.FIBER_CUT_PROBABLE;
            severity = IncidentSeverity.CRITICAL;
            title = "Rompimento Troncal Provável: " + pon.getPonName() + " (" + affectedCtos.size() + " CTOs Inoperantes)";
        } else {
            type = IncidentType.CTO_OFFLINE;
            severity = IncidentSeverity.MAJOR;
            title = "CTO Inoperante / Rompimento de Distribuição: " + pon.getPonName();
        }

        // Calcula coordenada geográfica média das CTOs afetadas
        BigDecimal avgLat = BigDecimal.ZERO;
        BigDecimal avgLng = BigDecimal.ZERO;
        int count = 0;

        for (UUID ctoId : affectedCtos) {
            Optional<FtthCto> cto = ctoRepository.findById(ctoId);
            if (cto.isPresent()) {
                avgLat = avgLat.add(cto.get().getLatitude());
                avgLng = avgLng.add(cto.get().getLongitude());
                count++;
            }
        }

        if (count > 0) {
            avgLat = avgLat.divide(BigDecimal.valueOf(count), 8, java.math.RoundingMode.HALF_UP);
            avgLng = avgLng.divide(BigDecimal.valueOf(count), 8, java.math.RoundingMode.HALF_UP);
        }

        String jsonCtos;
        try {
            jsonCtos = objectMapper.writeValueAsString(affectedCtos);
        } catch (Exception e) {
            jsonCtos = "[]";
        }

        FtthIncident incident = FtthIncident.builder()
                .companyId(pon.getCompanyId())
                .networkDeviceId(pon.getNetworkDeviceId())
                .oltPonPortId(pon.getId())
                .incidentType(type)
                .severity(severity)
                .status(IncidentStatus.ACTIVE)
                .title(title)
                .description("Detecção automática via correlação de telemetria óptica. " + totalAffectedCustomers + " assinantes sem sinal (LOS).")
                .affectedCustomersCount(totalAffectedCustomers)
                .affectedCtosIds(jsonCtos)
                .affectedCableId(pon.getConnectedCableId())
                .estimatedCutLatitude(count > 0 ? avgLat : null)
                .estimatedCutLongitude(count > 0 ? avgLng : null)
                .estimatedCutDetails("Trecho de fibra entre OLT e as CTOs da PON " + pon.getPonName())
                .detectedAt(OffsetDateTime.now())
                .build();

        return incidentRepository.save(incident);
    }

    @Transactional(readOnly = true)
    public List<FtthIncidentResponse> getActiveIncidents() {
        return incidentRepository.findByStatusInOrderByDetectedAtDesc(
                List.of(IncidentStatus.ACTIVE, IncidentStatus.INVESTIGATING, IncidentStatus.DISPATCHED)
        ).stream().map(this::enrichIncidentResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<FtthIncidentResponse> getAllIncidents() {
        return incidentRepository.findAllByOrderByDetectedAtDesc().stream()
                .map(this::enrichIncidentResponse)
                .toList();
    }

    @Transactional
    public FtthIncidentResponse dispatchIncident(UUID incidentId, FtthIncidentDispatchRequest request) {
        FtthIncident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incidente não encontrado: " + incidentId));

        incident.setStatus(IncidentStatus.DISPATCHED);
        incident.setDispatchedAt(OffsetDateTime.now());
        if (request.getNotes() != null) {
            incident.setDescription(incident.getDescription() + " | Despacho: " + request.getNotes());
        }

        incident = incidentRepository.save(incident);
        return enrichIncidentResponse(incident);
    }

    @Transactional
    public FtthIncidentResponse resolveIncident(UUID incidentId, FtthIncidentResolveRequest request) {
        FtthIncident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incidente não encontrado: " + incidentId));

        incident.setStatus(IncidentStatus.RESOLVED);
        incident.setResolvedAt(OffsetDateTime.now());
        incident.setRootCauseNotes(request.getRootCauseNotes());

        incident = incidentRepository.save(incident);
        return enrichIncidentResponse(incident);
    }

    @Transactional(readOnly = true)
    public NocMonitoringSummaryResponse getMonitoringSummary() {
        List<NetworkDevice> olts = networkDeviceRepository.findAll().stream()
                .filter(d -> "OLT".equalsIgnoreCase(d.getDeviceType()))
                .toList();

        List<OltPonPort> ponPorts = oltPonPortRepository.findAll();
        long activePons = ponPorts.stream().filter(p -> "ACTIVE".equalsIgnoreCase(p.getOperStatus())).count();

        int totalOnus = 0;
        int onlineOnus = 0;
        int losOnus = 0;
        int dyingGaspOnus = 0;
        int offlineOnus = 0;

        for (OltPonPort p : ponPorts) {
            totalOnus += p.getTotalOnus();
            onlineOnus += p.getOnlineOnus();
            losOnus += p.getLosOnus();
            dyingGaspOnus += p.getDyingGaspOnus();
            offlineOnus += p.getOfflineOnus();
        }

        double health = totalOnus > 0 ? ((double) onlineOnus / totalOnus) * 100.0 : 100.0;
        List<FtthIncidentResponse> activeIncidents = getActiveIncidents();
        long criticalCount = activeIncidents.stream().filter(i -> i.getSeverity() == IncidentSeverity.CRITICAL).count();

        return NocMonitoringSummaryResponse.builder()
                .totalOlts(olts.size())
                .totalPonPorts(ponPorts.size())
                .activePonPorts((int) activePons)
                .totalOnus(totalOnus)
                .onlineOnus(onlineOnus)
                .losOnus(losOnus)
                .dyingGaspOnus(dyingGaspOnus)
                .offlineOnus(offlineOnus)
                .globalHealthPercentage(Math.round(health * 10.0) / 10.0)
                .activeIncidentsCount(activeIncidents.size())
                .criticalIncidentsCount((int) criticalCount)
                .activeIncidents(activeIncidents)
                .build();
    }

    private FtthIncidentResponse enrichIncidentResponse(FtthIncident incident) {
        FtthIncidentResponse resp = mapper.toIncidentResponse(incident);

        if (incident.getNetworkDeviceId() != null) {
            networkDeviceRepository.findById(incident.getNetworkDeviceId()).ifPresent(d -> resp.setOltName(d.getName()));
        }
        if (incident.getOltPonPortId() != null) {
            oltPonPortRepository.findById(incident.getOltPonPortId()).ifPresent(p -> resp.setPonName(p.getPonName()));
        }
        if (incident.getAffectedCableId() != null) {
            cableRepository.findById(incident.getAffectedCableId()).ifPresent(c -> resp.setAffectedCableName(c.getName()));
        }

        // Decodifica nomes das CTOs afetadas
        if (incident.getAffectedCtosIds() != null) {
            try {
                List<String> ctoIds = objectMapper.readValue(incident.getAffectedCtosIds(), new TypeReference<List<String>>() {});
                List<String> ctoNames = new ArrayList<>();
                for (Object idObj : ctoIds) {
                    UUID id = UUID.fromString(idObj.toString());
                    ctoRepository.findById(id).ifPresent(c -> ctoNames.add(c.getName()));
                }
                resp.setAffectedCtoNames(ctoNames);
            } catch (Exception ignored) {}
        }

        return resp;
    }
}
