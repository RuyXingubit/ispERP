package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.contract.FtthMonitoringApi;
import br.dev.xb.isperp.api.dto.*;
import br.dev.xb.isperp.service.FtthCorrelationEngine;
import br.dev.xb.isperp.service.OltTelemetryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"/ftth/monitoring", "/api/ftth/monitoring"})
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@SuppressWarnings("null")
public class FtthMonitoringController implements FtthMonitoringApi {

    private final OltTelemetryService telemetryService;
    private final FtthCorrelationEngine correlationEngine;

    @Override
    @GetMapping({"/summary", "/api/ftth/monitoring/summary"})
    public ResponseEntity<NocMonitoringSummaryResponse> getMonitoringSummary() {
        return ResponseEntity.ok(toApiSummaryResponse(correlationEngine.getMonitoringSummary()));
    }

    @Override
    @GetMapping({"/pons", "/api/ftth/monitoring/pons"})
    public ResponseEntity<List<OltPonPortResponse>> getAllPonPorts() {
        return ResponseEntity.ok(telemetryService.getAllPonPorts().stream()
                .map(this::toApiPonResponse)
                .collect(Collectors.toList()));
    }

    @Override
    @GetMapping({"/pons/device/{deviceId}", "/api/ftth/monitoring/pons/device/{deviceId}"})
    public ResponseEntity<List<OltPonPortResponse>> getPonPortsByDevice(@PathVariable UUID deviceId) {
        return ResponseEntity.ok(telemetryService.getPonPortsByDevice(deviceId).stream()
                .map(this::toApiPonResponse)
                .collect(Collectors.toList()));
    }

    @Override
    @PostMapping({"/pons", "/api/ftth/monitoring/pons"})
    public ResponseEntity<OltPonPortResponse> createPonPort(@Valid @RequestBody OltPonPortRequest request) {
        br.dev.xb.isperp.dto.OltPonPortRequest internalReq = br.dev.xb.isperp.dto.OltPonPortRequest.builder()
                .companyId(request.getCompanyId())
                .networkDeviceId(request.getNetworkDeviceId())
                .slotNumber(request.getSlotNumber() != null ? request.getSlotNumber() : 0)
                .portNumber(request.getPortNumber() != null ? request.getPortNumber() : 1)
                .ponName(request.getPonName())
                .connectedCableId(request.getConnectedCableId())
                .txPowerDbm(request.getTxPowerDbm() != null ? BigDecimal.valueOf(request.getTxPowerDbm()) : null)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(toApiPonResponse(telemetryService.createPonPort(internalReq)));
    }

    @Override
    @GetMapping({"/incidents", "/api/ftth/monitoring/incidents"})
    public ResponseEntity<List<FtthIncidentResponse>> getAllIncidents() {
        return ResponseEntity.ok(correlationEngine.getAllIncidents().stream()
                .map(this::toApiIncidentResponse)
                .collect(Collectors.toList()));
    }

    @Override
    @GetMapping({"/incidents/active", "/api/ftth/monitoring/incidents/active"})
    public ResponseEntity<List<FtthIncidentResponse>> getActiveIncidents() {
        return ResponseEntity.ok(correlationEngine.getActiveIncidents().stream()
                .map(this::toApiIncidentResponse)
                .collect(Collectors.toList()));
    }

    @Override
    @PostMapping({"/incidents/{id}/dispatch", "/api/ftth/monitoring/incidents/{id}/dispatch"})
    public ResponseEntity<FtthIncidentResponse> dispatchIncident(
            @PathVariable UUID id,
            @RequestBody FtthIncidentDispatchRequest request
    ) {
        br.dev.xb.isperp.dto.FtthIncidentDispatchRequest internalReq = br.dev.xb.isperp.dto.FtthIncidentDispatchRequest.builder()
                .technicianId(request.getTechnicianId())
                .notes(request.getNotes())
                .build();
        return ResponseEntity.ok(toApiIncidentResponse(correlationEngine.dispatchIncident(id, internalReq)));
    }

    @Override
    @PostMapping({"/incidents/{id}/resolve", "/api/ftth/monitoring/incidents/{id}/resolve"})
    public ResponseEntity<FtthIncidentResponse> resolveIncident(
            @PathVariable UUID id,
            @Valid @RequestBody FtthIncidentResolveRequest request
    ) {
        br.dev.xb.isperp.dto.FtthIncidentResolveRequest internalReq = br.dev.xb.isperp.dto.FtthIncidentResolveRequest.builder()
                .rootCauseNotes(request.getRootCauseNotes())
                .build();
        return ResponseEntity.ok(toApiIncidentResponse(correlationEngine.resolveIncident(id, internalReq)));
    }

    @Override
    @PostMapping({"/poll-now", "/api/ftth/monitoring/poll-now"})
    public ResponseEntity<NocMonitoringSummaryResponse> forcePollCycle() {
        telemetryService.pollOltPonSummaries();
        correlationEngine.runCorrelationAnalysis();
        return ResponseEntity.ok(toApiSummaryResponse(correlationEngine.getMonitoringSummary()));
    }

    private NocMonitoringSummaryResponse toApiSummaryResponse(br.dev.xb.isperp.dto.NocMonitoringSummaryResponse res) {
        if (res == null) return null;
        NocMonitoringSummaryResponse api = new NocMonitoringSummaryResponse();
        api.setTotalOlts(res.getTotalOlts());
        api.setTotalPonPorts(res.getTotalPonPorts());
        api.setActivePonPorts(res.getActivePonPorts());
        api.setTotalOnus(res.getTotalOnus());
        api.setOnlineOnus(res.getOnlineOnus());
        api.setLosOnus(res.getLosOnus());
        api.setDyingGaspOnus(res.getDyingGaspOnus());
        api.setOfflineOnus(res.getOfflineOnus());
        api.setGlobalHealthPercentage(res.getGlobalHealthPercentage());
        api.setActiveIncidentsCount(res.getActiveIncidentsCount());
        api.setCriticalIncidentsCount(res.getCriticalIncidentsCount());
        if (res.getActiveIncidents() != null) {
            api.setActiveIncidents(res.getActiveIncidents().stream().map(this::toApiIncidentResponse).collect(Collectors.toList()));
        }
        return api;
    }

    private OltPonPortResponse toApiPonResponse(br.dev.xb.isperp.dto.OltPonPortResponse res) {
        if (res == null) return null;
        OltPonPortResponse api = new OltPonPortResponse();
        api.setId(res.getId());
        api.setCompanyId(res.getCompanyId());
        api.setNetworkDeviceId(res.getNetworkDeviceId());
        api.setOltName(res.getOltName());
        api.setSlotNumber(res.getSlotNumber());
        api.setPortNumber(res.getPortNumber());
        api.setPonName(res.getPonName());
        api.setAdminStatus(res.getAdminStatus());
        api.setOperStatus(res.getOperStatus());
        api.setTxPowerDbm(res.getTxPowerDbm() != null ? res.getTxPowerDbm().doubleValue() : null);
        api.setTemperatureCelsius(res.getTemperatureCelsius() != null ? res.getTemperatureCelsius().doubleValue() : null);
        api.setTotalOnus(res.getTotalOnus());
        api.setOnlineOnus(res.getOnlineOnus());
        api.setLosOnus(res.getLosOnus());
        api.setDyingGaspOnus(res.getDyingGaspOnus());
        api.setOfflineOnus(res.getOfflineOnus());
        api.setHealthPercentage(res.getHealthPercentage());
        api.setConnectedCableId(res.getConnectedCableId());
        api.setConnectedCableName(res.getConnectedCableName());
        if (res.getLastPolledAt() != null) {
            api.setLastPolledAt(res.getLastPolledAt().atZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime());
        }
        if (res.getCreatedAt() != null) {
            api.setCreatedAt(res.getCreatedAt().atZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime());
        }
        return api;
    }

    private FtthIncidentResponse toApiIncidentResponse(br.dev.xb.isperp.dto.FtthIncidentResponse res) {
        if (res == null) return null;
        FtthIncidentResponse api = new FtthIncidentResponse();
        api.setId(res.getId());
        api.setCompanyId(res.getCompanyId());
        api.setNetworkDeviceId(res.getNetworkDeviceId());
        api.setOltName(res.getOltName());
        api.setOltPonPortId(res.getOltPonPortId());
        api.setPonName(res.getPonName());
        if (res.getIncidentType() != null) {
            api.setIncidentType(IncidentType.fromValue(res.getIncidentType().name()));
        }
        api.setIncidentTypeDescription(res.getIncidentTypeDescription());
        if (res.getSeverity() != null) {
            api.setSeverity(IncidentSeverity.fromValue(res.getSeverity().name()));
        }
        if (res.getStatus() != null) {
            api.setStatus(IncidentStatus.fromValue(res.getStatus().name()));
        }
        api.setTitle(res.getTitle());
        api.setDescription(res.getDescription());
        api.setAffectedCustomersCount(res.getAffectedCustomersCount());
        api.setAffectedCtosIds(res.getAffectedCtosIds());
        api.setAffectedCtoNames(res.getAffectedCtoNames());
        api.setAffectedCableId(res.getAffectedCableId());
        api.setAffectedCableName(res.getAffectedCableName());
        api.setEstimatedCutLatitude(res.getEstimatedCutLatitude() != null ? res.getEstimatedCutLatitude().doubleValue() : null);
        api.setEstimatedCutLongitude(res.getEstimatedCutLongitude() != null ? res.getEstimatedCutLongitude().doubleValue() : null);
        api.setEstimatedCutDetails(res.getEstimatedCutDetails());
        api.setWorkOrderId(res.getWorkOrderId());
        api.setWorkOrderProtocol(res.getWorkOrderProtocol());
        if (res.getDetectedAt() != null) {
            api.setDetectedAt(res.getDetectedAt().atZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime());
        }
        if (res.getDispatchedAt() != null) {
            api.setDispatchedAt(res.getDispatchedAt().atZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime());
        }
        if (res.getResolvedAt() != null) {
            api.setResolvedAt(res.getResolvedAt().atZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime());
        }
        api.setRootCauseNotes(res.getRootCauseNotes());
        return api;
    }
}
