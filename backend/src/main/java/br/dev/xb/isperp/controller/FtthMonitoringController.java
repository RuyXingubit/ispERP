package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.dto.*;
import br.dev.xb.isperp.service.FtthCorrelationEngine;
import br.dev.xb.isperp.service.OltTelemetryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/ftth/monitoring", "/api/ftth/monitoring"})
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@SuppressWarnings("null")
public class FtthMonitoringController {

    private final OltTelemetryService telemetryService;
    private final FtthCorrelationEngine correlationEngine;

    @GetMapping("/summary")
    public ResponseEntity<NocMonitoringSummaryResponse> getMonitoringSummary() {
        return ResponseEntity.ok(correlationEngine.getMonitoringSummary());
    }

    @GetMapping("/pons")
    public ResponseEntity<List<OltPonPortResponse>> getAllPonPorts() {
        return ResponseEntity.ok(telemetryService.getAllPonPorts());
    }

    @GetMapping("/pons/device/{deviceId}")
    public ResponseEntity<List<OltPonPortResponse>> getPonPortsByDevice(@PathVariable UUID deviceId) {
        return ResponseEntity.ok(telemetryService.getPonPortsByDevice(deviceId));
    }

    @PostMapping("/pons")
    public ResponseEntity<OltPonPortResponse> createPonPort(@Valid @RequestBody OltPonPortRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(telemetryService.createPonPort(request));
    }

    @GetMapping("/incidents")
    public ResponseEntity<List<FtthIncidentResponse>> getAllIncidents() {
        return ResponseEntity.ok(correlationEngine.getAllIncidents());
    }

    @GetMapping("/incidents/active")
    public ResponseEntity<List<FtthIncidentResponse>> getActiveIncidents() {
        return ResponseEntity.ok(correlationEngine.getActiveIncidents());
    }

    @PostMapping("/incidents/{id}/dispatch")
    public ResponseEntity<FtthIncidentResponse> dispatchIncident(
            @PathVariable UUID id,
            @RequestBody FtthIncidentDispatchRequest request
    ) {
        return ResponseEntity.ok(correlationEngine.dispatchIncident(id, request));
    }

    @PostMapping("/incidents/{id}/resolve")
    public ResponseEntity<FtthIncidentResponse> resolveIncident(
            @PathVariable UUID id,
            @Valid @RequestBody FtthIncidentResolveRequest request
    ) {
        return ResponseEntity.ok(correlationEngine.resolveIncident(id, request));
    }

    @PostMapping("/poll-now")
    public ResponseEntity<NocMonitoringSummaryResponse> forcePollCycle() {
        telemetryService.pollOltPonSummaries();
        correlationEngine.runCorrelationAnalysis();
        return ResponseEntity.ok(correlationEngine.getMonitoringSummary());
    }
}
