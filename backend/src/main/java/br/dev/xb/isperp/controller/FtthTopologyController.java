package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.contract.FtthTopologyApi;
import br.dev.xb.isperp.api.dto.*;
import br.dev.xb.isperp.service.FtthFusionService;
import br.dev.xb.isperp.service.FtthLightPathService;
import br.dev.xb.isperp.service.FtthTopologyService;
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
@RequestMapping({"/ftth", "/api/ftth"})
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@SuppressWarnings("null")
public class FtthTopologyController implements FtthTopologyApi {

    private final FtthTopologyService topologyService;
    private final FtthFusionService fusionService;
    private final FtthLightPathService lightPathService;

    // --- POPs ---
    @Override
    @GetMapping({"/pops", "/api/ftth/pops"})
    public ResponseEntity<List<FtthPopResponse>> getAllPops() {
        return ResponseEntity.ok(topologyService.getAllPops().stream()
                .map(this::toApiPopResponse)
                .collect(Collectors.toList()));
    }

    @Override
    @PostMapping({"/pops", "/api/ftth/pops"})
    public ResponseEntity<FtthPopResponse> createPop(@Valid @RequestBody FtthPopRequest request) {
        br.dev.xb.isperp.dto.FtthPopRequest internal = br.dev.xb.isperp.dto.FtthPopRequest.builder()
                .companyId(request.getCompanyId())
                .name(request.getName())
                .latitude(request.getLatitude() != null ? BigDecimal.valueOf(request.getLatitude()) : null)
                .longitude(request.getLongitude() != null ? BigDecimal.valueOf(request.getLongitude()) : null)
                .address(request.getAddress())
                .description(request.getDescription())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(toApiPopResponse(topologyService.createPop(internal)));
    }

    // --- Postes ---
    @Override
    @GetMapping({"/poles", "/api/ftth/poles"})
    public ResponseEntity<List<FtthPoleResponse>> getAllPoles() {
        return ResponseEntity.ok(topologyService.getAllPoles().stream()
                .map(this::toApiPoleResponse)
                .collect(Collectors.toList()));
    }

    @Override
    @PostMapping({"/poles", "/api/ftth/poles"})
    public ResponseEntity<FtthPoleResponse> createPole(@Valid @RequestBody FtthPoleRequest request) {
        br.dev.xb.isperp.dto.FtthPoleRequest internal = br.dev.xb.isperp.dto.FtthPoleRequest.builder()
                .companyId(request.getCompanyId())
                .code(request.getCode())
                .latitude(request.getLatitude() != null ? BigDecimal.valueOf(request.getLatitude()) : null)
                .longitude(request.getLongitude() != null ? BigDecimal.valueOf(request.getLongitude()) : null)
                .poleType(request.getPoleType() != null ? request.getPoleType() : "CONCRETO")
                .reservationMeters(request.getReservationMeters() != null ? request.getReservationMeters() : 0)
                .description(request.getDescription())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(toApiPoleResponse(topologyService.createPole(internal)));
    }

    // --- Cabos ---
    @Override
    @GetMapping({"/cables", "/api/ftth/cables"})
    public ResponseEntity<List<FtthCableResponse>> getAllCables() {
        return ResponseEntity.ok(topologyService.getAllCables().stream()
                .map(this::toApiCableResponse)
                .collect(Collectors.toList()));
    }

    @Override
    @GetMapping({"/cables/{id}", "/api/ftth/cables/{id}"})
    public ResponseEntity<FtthCableResponse> getCableById(@PathVariable UUID id) {
        return ResponseEntity.ok(toApiCableResponse(topologyService.getCableById(id)));
    }

    @Override
    @PostMapping({"/cables", "/api/ftth/cables"})
    public ResponseEntity<FtthCableResponse> createCable(@Valid @RequestBody FtthCableRequest request) {
        br.dev.xb.isperp.dto.FtthCableRequest internal = br.dev.xb.isperp.dto.FtthCableRequest.builder()
                .companyId(request.getCompanyId())
                .name(request.getName())
                .cableType(request.getCableType() != null ? br.dev.xb.isperp.ftth.FtthCableType.valueOf(request.getCableType().name()) : br.dev.xb.isperp.ftth.FtthCableType.DISTRIBUICAO)
                .fiberCount(request.getFiberCount() != null ? request.getFiberCount() : 12)
                .tubeCount(request.getTubeCount() != null ? request.getTubeCount() : 1)
                .colorStandard(request.getColorStandard() != null ? br.dev.xb.isperp.ftth.FiberColorStandard.valueOf(request.getColorStandard().name()) : br.dev.xb.isperp.ftth.FiberColorStandard.ABNT_NBR_14106)
                .lengthMeters(request.getLengthMeters() != null ? BigDecimal.valueOf(request.getLengthMeters()) : BigDecimal.ZERO)
                .pathCoordinates(request.getPathCoordinates())
                .sourcePopId(request.getSourcePopId())
                .sourcePoleId(request.getSourcePoleId())
                .targetPoleId(request.getTargetPoleId())
                .attenuationDbPerKm(request.getAttenuationDbPerKm() != null ? BigDecimal.valueOf(request.getAttenuationDbPerKm()) : new BigDecimal("0.35"))
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(toApiCableResponse(topologyService.createCable(internal)));
    }

    // --- Caixas de Emenda (CEO) ---
    @Override
    @GetMapping({"/closures", "/api/ftth/closures"})
    public ResponseEntity<List<FtthClosureResponse>> getAllClosures() {
        return ResponseEntity.ok(topologyService.getAllClosures().stream()
                .map(this::toApiClosureResponse)
                .collect(Collectors.toList()));
    }

    @Override
    @GetMapping({"/closures/{id}", "/api/ftth/closures/{id}"})
    public ResponseEntity<FtthClosureResponse> getClosureById(@PathVariable UUID id) {
        return ResponseEntity.ok(toApiClosureResponse(topologyService.getClosureById(id)));
    }

    @Override
    @PostMapping({"/closures", "/api/ftth/closures"})
    public ResponseEntity<FtthClosureResponse> createClosure(@Valid @RequestBody FtthClosureRequest request) {
        br.dev.xb.isperp.dto.FtthClosureRequest internal = br.dev.xb.isperp.dto.FtthClosureRequest.builder()
                .companyId(request.getCompanyId())
                .name(request.getName())
                .poleId(request.getPoleId())
                .latitude(request.getLatitude() != null ? BigDecimal.valueOf(request.getLatitude()) : null)
                .longitude(request.getLongitude() != null ? BigDecimal.valueOf(request.getLongitude()) : null)
                .closureType(request.getClosureType() != null ? br.dev.xb.isperp.ftth.FtthClosureType.valueOf(request.getClosureType().name()) : br.dev.xb.isperp.ftth.FtthClosureType.DOMO)
                .trayCount(request.getTrayCount() != null ? request.getTrayCount() : 4)
                .capacityFusions(request.getCapacityFusions() != null ? request.getCapacityFusions() : 48)
                .status(request.getStatus() != null ? request.getStatus() : "ATIVA")
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(toApiClosureResponse(topologyService.createClosure(internal)));
    }

    @Override
    @GetMapping({"/closures/{id}/diagram", "/api/ftth/closures/{id}/diagram"})
    public ResponseEntity<FtthClosureDiagramResponse> getClosureDiagram(@PathVariable UUID id) {
        return ResponseEntity.ok(toApiDiagramResponse(fusionService.getClosureDiagram(id)));
    }

    // --- Splitters & Fusões ---
    @Override
    @PostMapping({"/closures/{id}/splitters", "/api/ftth/closures/{id}/splitters"})
    public ResponseEntity<FtthSplitterResponse> createSplitter(
            @PathVariable UUID id,
            @Valid @RequestBody FtthSplitterRequest request
    ) {
        br.dev.xb.isperp.dto.FtthSplitterRequest internal = br.dev.xb.isperp.dto.FtthSplitterRequest.builder()
                .companyId(request.getCompanyId())
                .closureId(id)
                .name(request.getName())
                .splitterType(request.getSplitterType() != null ? br.dev.xb.isperp.ftth.FtthSplitterType.valueOf(request.getSplitterType().name()) : br.dev.xb.isperp.ftth.FtthSplitterType.BALANCED_1_8)
                .inputCableId(request.getInputCableId())
                .inputFiberNumber(request.getInputFiberNumber())
                .attenuationDb(request.getAttenuationDb() != null ? BigDecimal.valueOf(request.getAttenuationDb()) : null)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(toApiSplitterResponse(topologyService.createSplitter(internal)));
    }

    @Override
    @PostMapping({"/closures/{id}/fusions", "/api/ftth/closures/{id}/fusions"})
    public ResponseEntity<FtthFusionResponse> createFusion(
            @PathVariable UUID id,
            @Valid @RequestBody FtthFusionRequest request
    ) {
        br.dev.xb.isperp.dto.FtthFusionRequest internal = br.dev.xb.isperp.dto.FtthFusionRequest.builder()
                .closureId(id)
                .trayNumber(request.getTrayNumber() != null ? request.getTrayNumber() : 1)
                .sourceCableId(request.getSourceCableId())
                .sourceFiberNumber(request.getSourceFiberNumber() != null ? request.getSourceFiberNumber() : 1)
                .targetCableId(request.getTargetCableId())
                .targetFiberNumber(request.getTargetFiberNumber())
                .targetSplitterId(request.getTargetSplitterId())
                .targetCtoId(request.getTargetCtoId())
                .lossDb(request.getLossDb() != null ? BigDecimal.valueOf(request.getLossDb()) : new BigDecimal("0.05"))
                .description(request.getDescription())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(toApiFusionResponse(fusionService.createFusion(internal)));
    }

    @Override
    @DeleteMapping({"/fusions/{id}", "/api/ftth/fusions/{id}"})
    public ResponseEntity<Void> deleteFusion(@PathVariable UUID id) {
        fusionService.deleteFusion(id);
        return ResponseEntity.noContent().build();
    }

    // --- Caixas de Atendimento (CTO) ---
    @Override
    @GetMapping({"/ctos", "/api/ftth/ctos"})
    public ResponseEntity<List<FtthCtoResponse>> getAllCtos() {
        return ResponseEntity.ok(topologyService.getAllCtos().stream()
                .map(this::toApiCtoResponse)
                .collect(Collectors.toList()));
    }

    @Override
    @GetMapping({"/ctos/{id}", "/api/ftth/ctos/{id}"})
    public ResponseEntity<FtthCtoResponse> getCtoById(@PathVariable UUID id) {
        return ResponseEntity.ok(toApiCtoResponse(topologyService.getCtoById(id)));
    }

    @Override
    @PostMapping({"/ctos", "/api/ftth/ctos"})
    public ResponseEntity<FtthCtoResponse> createCto(@Valid @RequestBody FtthCtoRequest request) {
        br.dev.xb.isperp.dto.FtthCtoRequest internal = br.dev.xb.isperp.dto.FtthCtoRequest.builder()
                .companyId(request.getCompanyId())
                .name(request.getName())
                .poleId(request.getPoleId())
                .closureId(request.getClosureId())
                .latitude(request.getLatitude() != null ? BigDecimal.valueOf(request.getLatitude()) : null)
                .longitude(request.getLongitude() != null ? BigDecimal.valueOf(request.getLongitude()) : null)
                .totalPorts(request.getTotalPorts() != null ? request.getTotalPorts() : 16)
                .splitterType(request.getSplitterType() != null ? request.getSplitterType() : "BALANCED_1_16")
                .status(request.getStatus() != null ? request.getStatus() : "ATIVA")
                .description(request.getDescription())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(toApiCtoResponse(topologyService.createCto(internal)));
    }

    // --- Viabilidade de Vendas & Rastreamento Óptico ---
    @Override
    @PostMapping({"/feasibility", "/api/ftth/feasibility"})
    public ResponseEntity<FtthFeasibilityResponse> checkFeasibility(@Valid @RequestBody FtthFeasibilityRequest request) {
        br.dev.xb.isperp.dto.FtthFeasibilityRequest internal = br.dev.xb.isperp.dto.FtthFeasibilityRequest.builder()
                .latitude(request.getLatitude() != null ? BigDecimal.valueOf(request.getLatitude()) : null)
                .longitude(request.getLongitude() != null ? BigDecimal.valueOf(request.getLongitude()) : null)
                .maxDistanceMeters(request.getMaxDistanceMeters() != null ? request.getMaxDistanceMeters() : 200.0)
                .build();
        return ResponseEntity.ok(toApiFeasibilityResponse(topologyService.calculateFeasibility(internal)));
    }

    @Override
    @GetMapping({"/lightpath/{ctoPortId}", "/api/ftth/lightpath/{ctoPortId}"})
    public ResponseEntity<LightPathTraceResult> traceLightPath(@PathVariable UUID ctoPortId) {
        return ResponseEntity.ok(toApiLightPathTraceResult(lightPathService.traceLightPathFromCtoPort(ctoPortId)));
    }

    // --- Helper Mappers ---

    private FtthPopResponse toApiPopResponse(br.dev.xb.isperp.dto.FtthPopResponse res) {
        if (res == null) return null;
        FtthPopResponse api = new FtthPopResponse();
        api.setId(res.getId());
        api.setCompanyId(res.getCompanyId());
        api.setName(res.getName());
        api.setLatitude(res.getLatitude() != null ? res.getLatitude().doubleValue() : null);
        api.setLongitude(res.getLongitude() != null ? res.getLongitude().doubleValue() : null);
        api.setAddress(res.getAddress());
        api.setDescription(res.getDescription());
        if (res.getCreatedAt() != null) api.setCreatedAt(res.getCreatedAt().atZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime());
        if (res.getUpdatedAt() != null) api.setUpdatedAt(res.getUpdatedAt().atZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime());
        return api;
    }

    private FtthPoleResponse toApiPoleResponse(br.dev.xb.isperp.dto.FtthPoleResponse res) {
        if (res == null) return null;
        FtthPoleResponse api = new FtthPoleResponse();
        api.setId(res.getId());
        api.setCompanyId(res.getCompanyId());
        api.setCode(res.getCode());
        api.setLatitude(res.getLatitude() != null ? res.getLatitude().doubleValue() : null);
        api.setLongitude(res.getLongitude() != null ? res.getLongitude().doubleValue() : null);
        api.setPoleType(res.getPoleType());
        api.setReservationMeters(res.getReservationMeters());
        api.setDescription(res.getDescription());
        if (res.getCreatedAt() != null) api.setCreatedAt(res.getCreatedAt().atZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime());
        if (res.getUpdatedAt() != null) api.setUpdatedAt(res.getUpdatedAt().atZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime());
        return api;
    }

    private FtthCableResponse toApiCableResponse(br.dev.xb.isperp.dto.FtthCableResponse res) {
        if (res == null) return null;
        FtthCableResponse api = new FtthCableResponse();
        api.setId(res.getId());
        api.setCompanyId(res.getCompanyId());
        api.setName(res.getName());
        if (res.getCableType() != null) api.setCableType(FtthCableType.fromValue(res.getCableType().name()));
        api.setFiberCount(res.getFiberCount());
        api.setTubeCount(res.getTubeCount());
        if (res.getColorStandard() != null) api.setColorStandard(FiberColorStandard.fromValue(res.getColorStandard().name()));
        api.setLengthMeters(res.getLengthMeters() != null ? res.getLengthMeters().doubleValue() : null);
        api.setPathCoordinates(res.getPathCoordinates());
        api.setSourcePopId(res.getSourcePopId());
        api.setSourcePoleId(res.getSourcePoleId());
        api.setTargetPoleId(res.getTargetPoleId());
        api.setAttenuationDbPerKm(res.getAttenuationDbPerKm() != null ? res.getAttenuationDbPerKm().doubleValue() : null);
        if (res.getFibers() != null) {
            api.setFibers(res.getFibers().stream().map(this::toApiFiberInfo).collect(Collectors.toList()));
        }
        if (res.getCreatedAt() != null) api.setCreatedAt(res.getCreatedAt().atZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime());
        if (res.getUpdatedAt() != null) api.setUpdatedAt(res.getUpdatedAt().atZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime());
        return api;
    }

    private FiberColorInfo toApiFiberInfo(br.dev.xb.isperp.ftth.FiberColorInfo info) {
        if (info == null) return null;
        FiberColorInfo api = new FiberColorInfo();
        api.setFiberNumber(info.getFiberNumber());
        api.setTubeNumber(info.getTubeNumber());
        api.setFiberInTubeNumber(info.getFiberInTubeNumber());
        api.setFiberColorName(info.getFiberColorName());
        api.setFiberColorHex(info.getFiberColorHex());
        api.setTubeColorName(info.getTubeColorName());
        api.setTubeColorHex(info.getTubeColorHex());
        if (info.getStandard() != null) api.setStandard(FiberColorStandard.fromValue(info.getStandard().name()));
        return api;
    }

    private FtthClosureResponse toApiClosureResponse(br.dev.xb.isperp.dto.FtthClosureResponse res) {
        if (res == null) return null;
        FtthClosureResponse api = new FtthClosureResponse();
        api.setId(res.getId());
        api.setCompanyId(res.getCompanyId());
        api.setName(res.getName());
        api.setPoleId(res.getPoleId());
        api.setPoleCode(res.getPoleCode());
        api.setLatitude(res.getLatitude() != null ? res.getLatitude().doubleValue() : null);
        api.setLongitude(res.getLongitude() != null ? res.getLongitude().doubleValue() : null);
        if (res.getClosureType() != null) api.setClosureType(FtthClosureType.fromValue(res.getClosureType().name()));
        api.setTrayCount(res.getTrayCount());
        api.setCapacityFusions(res.getCapacityFusions());
        api.setUsedFusionsCount(res.getUsedFusionsCount());
        api.setStatus(res.getStatus());
        if (res.getCreatedAt() != null) api.setCreatedAt(res.getCreatedAt().atZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime());
        if (res.getUpdatedAt() != null) api.setUpdatedAt(res.getUpdatedAt().atZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime());
        return api;
    }

    private FtthClosureDiagramResponse toApiDiagramResponse(br.dev.xb.isperp.dto.FtthClosureDiagramResponse res) {
        if (res == null) return null;
        FtthClosureDiagramResponse api = new FtthClosureDiagramResponse();
        api.setClosure(toApiClosureResponse(res.getClosure()));
        if (res.getCables() != null) {
            api.setCables(res.getCables().stream().map(this::toApiCableResponse).collect(Collectors.toList()));
        }
        if (res.getSplitters() != null) {
            api.setSplitters(res.getSplitters().stream().map(this::toApiSplitterResponse).collect(Collectors.toList()));
        }
        if (res.getFusions() != null) {
            api.setFusions(res.getFusions().stream().map(this::toApiFusionResponse).collect(Collectors.toList()));
        }
        if (res.getConnectedCtos() != null) {
            api.setConnectedCtos(res.getConnectedCtos().stream().map(this::toApiCtoResponse).collect(Collectors.toList()));
        }
        return api;
    }

    private FtthSplitterResponse toApiSplitterResponse(br.dev.xb.isperp.dto.FtthSplitterResponse res) {
        if (res == null) return null;
        FtthSplitterResponse api = new FtthSplitterResponse();
        api.setId(res.getId());
        api.setCompanyId(res.getCompanyId());
        api.setClosureId(res.getClosureId());
        api.setName(res.getName());
        if (res.getSplitterType() != null) api.setSplitterType(FtthSplitterType.fromValue(res.getSplitterType().name()));
        api.setInputCableId(res.getInputCableId());
        api.setInputFiberNumber(res.getInputFiberNumber());
        api.setAttenuationDb(res.getAttenuationDb() != null ? res.getAttenuationDb().doubleValue() : null);
        api.setOutputPorts(res.getOutputPorts());
        if (res.getCreatedAt() != null) api.setCreatedAt(res.getCreatedAt().atZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime());
        return api;
    }

    private FtthFusionResponse toApiFusionResponse(br.dev.xb.isperp.dto.FtthFusionResponse res) {
        if (res == null) return null;
        FtthFusionResponse api = new FtthFusionResponse();
        api.setId(res.getId());
        api.setClosureId(res.getClosureId());
        api.setTrayNumber(res.getTrayNumber());
        api.setSourceCableId(res.getSourceCableId());
        api.setSourceCableName(res.getSourceCableName());
        api.setSourceFiberNumber(res.getSourceFiberNumber());
        api.setSourceFiberColor(toApiFiberInfo(res.getSourceFiberColor()));
        api.setTargetCableId(res.getTargetCableId());
        api.setTargetCableName(res.getTargetCableName());
        api.setTargetFiberNumber(res.getTargetFiberNumber());
        api.setTargetFiberColor(toApiFiberInfo(res.getTargetFiberColor()));
        api.setTargetSplitterId(res.getTargetSplitterId());
        api.setTargetSplitterName(res.getTargetSplitterName());
        api.setTargetCtoId(res.getTargetCtoId());
        api.setTargetCtoName(res.getTargetCtoName());
        api.setLossDb(res.getLossDb() != null ? res.getLossDb().doubleValue() : null);
        api.setDescription(res.getDescription());
        if (res.getCreatedAt() != null) api.setCreatedAt(res.getCreatedAt().atZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime());
        return api;
    }

    private FtthCtoResponse toApiCtoResponse(br.dev.xb.isperp.dto.FtthCtoResponse res) {
        if (res == null) return null;
        FtthCtoResponse api = new FtthCtoResponse();
        api.setId(res.getId());
        api.setCompanyId(res.getCompanyId());
        api.setName(res.getName());
        api.setPoleId(res.getPoleId());
        api.setPoleCode(res.getPoleCode());
        api.setClosureId(res.getClosureId());
        api.setLatitude(res.getLatitude() != null ? res.getLatitude().doubleValue() : null);
        api.setLongitude(res.getLongitude() != null ? res.getLongitude().doubleValue() : null);
        api.setTotalPorts(res.getTotalPorts());
        api.setFreePortsCount(res.getFreePortsCount());
        api.setOccupiedPortsCount(res.getOccupiedPortsCount());
        api.setOccupancyPercentage(res.getOccupancyPercentage());
        api.setSplitterType(res.getSplitterType());
        api.setStatus(res.getStatus());
        api.setDescription(res.getDescription());
        if (res.getPorts() != null) {
            api.setPorts(res.getPorts().stream().map(this::toApiCtoPortResponse).collect(Collectors.toList()));
        }
        if (res.getCreatedAt() != null) api.setCreatedAt(res.getCreatedAt().atZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime());
        if (res.getUpdatedAt() != null) api.setUpdatedAt(res.getUpdatedAt().atZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime());
        return api;
    }

    private FtthCtoPortResponse toApiCtoPortResponse(br.dev.xb.isperp.dto.FtthCtoPortResponse res) {
        if (res == null) return null;
        FtthCtoPortResponse api = new FtthCtoPortResponse();
        api.setId(res.getId());
        api.setCtoId(res.getCtoId());
        api.setPortNumber(res.getPortNumber());
        if (res.getStatus() != null) api.setStatus(FtthPortStatus.fromValue(res.getStatus().name()));
        api.setOnuProvisioningId(res.getOnuProvisioningId());
        api.setOnuSerial(res.getOnuSerial());
        api.setOnuMac(res.getOnuMac());
        api.setCustomerId(res.getCustomerId());
        api.setCustomerName(res.getCustomerName());
        api.setPppoeUser(res.getPppoeUser());
        api.setNotes(res.getNotes());
        if (res.getCreatedAt() != null) api.setCreatedAt(res.getCreatedAt().atZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime());
        return api;
    }

    private FtthFeasibilityResponse toApiFeasibilityResponse(br.dev.xb.isperp.dto.FtthFeasibilityResponse res) {
        if (res == null) return null;
        FtthFeasibilityResponse api = new FtthFeasibilityResponse();
        api.setViable(res.isViable());
        api.setViableCtosCount(res.getViableCtosCount());
        if (res.getNearbyCtos() != null) {
            api.setNearbyCtos(res.getNearbyCtos().stream().map(item -> {
                FeasibleCtoItem feasible = new FeasibleCtoItem();
                feasible.setCto(toApiCtoResponse(item.getCto()));
                feasible.setDistanceMeters(item.getDistanceMeters());
                feasible.setFreePorts(item.getFreePorts());
                feasible.setHasCapacity(item.isHasCapacity());
                return feasible;
            }).collect(Collectors.toList()));
        }
        return api;
    }

    private LightPathTraceResult toApiLightPathTraceResult(FtthLightPathService.LightPathTraceResult res) {
        if (res == null) return null;
        LightPathTraceResult api = new LightPathTraceResult();
        api.setReachedSource(res.isReachedSource());
        api.setSourcePopName(res.getSourcePopName());
        api.setTotalAttenuationDb(res.getTotalAttenuationDb());
        api.setEstimatedRxPowerDbm(res.getEstimatedRxPowerDbm());
        if (res.getNodes() != null) {
            api.setNodes(res.getNodes().stream().map(node -> {
                LightPathNode n = new LightPathNode();
                n.setElementType(node.getElementType());
                n.setName(node.getName());
                n.setDetails(node.getDetails());
                n.setAddedAttenuationDb(node.getAddedAttenuationDb());
                n.setCumulativeAttenuationDb(node.getCumulativeAttenuationDb());
                return n;
            }).collect(Collectors.toList()));
        }
        return api;
    }
}
