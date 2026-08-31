package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.dto.*;
import br.dev.xb.isperp.service.FtthFusionService;
import br.dev.xb.isperp.service.FtthLightPathService;
import br.dev.xb.isperp.service.FtthTopologyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ftth")
@RequiredArgsConstructor
@SuppressWarnings("null")
public class FtthTopologyController {

    private final FtthTopologyService topologyService;
    private final FtthFusionService fusionService;
    private final FtthLightPathService lightPathService;

    // --- POPs ---
    @GetMapping("/pops")
    public ResponseEntity<List<FtthPopResponse>> getAllPops() {
        return ResponseEntity.ok(topologyService.getAllPops());
    }

    @PostMapping("/pops")
    public ResponseEntity<FtthPopResponse> createPop(@Valid @RequestBody FtthPopRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(topologyService.createPop(request));
    }

    // --- Postes ---
    @GetMapping("/poles")
    public ResponseEntity<List<FtthPoleResponse>> getAllPoles() {
        return ResponseEntity.ok(topologyService.getAllPoles());
    }

    @PostMapping("/poles")
    public ResponseEntity<FtthPoleResponse> createPole(@Valid @RequestBody FtthPoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(topologyService.createPole(request));
    }

    // --- Cabos ---
    @GetMapping("/cables")
    public ResponseEntity<List<FtthCableResponse>> getAllCables() {
        return ResponseEntity.ok(topologyService.getAllCables());
    }

    @GetMapping("/cables/{id}")
    public ResponseEntity<FtthCableResponse> getCableById(@PathVariable UUID id) {
        return ResponseEntity.ok(topologyService.getCableById(id));
    }

    @PostMapping("/cables")
    public ResponseEntity<FtthCableResponse> createCable(@Valid @RequestBody FtthCableRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(topologyService.createCable(request));
    }

    // --- Caixas de Emenda (CEO) ---
    @GetMapping("/closures")
    public ResponseEntity<List<FtthClosureResponse>> getAllClosures() {
        return ResponseEntity.ok(topologyService.getAllClosures());
    }

    @GetMapping("/closures/{id}")
    public ResponseEntity<FtthClosureResponse> getClosureById(@PathVariable UUID id) {
        return ResponseEntity.ok(topologyService.getClosureById(id));
    }

    @PostMapping("/closures")
    public ResponseEntity<FtthClosureResponse> createClosure(@Valid @RequestBody FtthClosureRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(topologyService.createClosure(request));
    }

    @GetMapping("/closures/{id}/diagram")
    public ResponseEntity<FtthClosureDiagramResponse> getClosureDiagram(@PathVariable UUID id) {
        return ResponseEntity.ok(fusionService.getClosureDiagram(id));
    }

    // --- Splitters & Fusões ---
    @PostMapping("/closures/{id}/splitters")
    public ResponseEntity<FtthSplitterResponse> createSplitter(
            @PathVariable UUID id,
            @Valid @RequestBody FtthSplitterRequest request
    ) {
        request.setClosureId(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(topologyService.createSplitter(request));
    }

    @PostMapping("/closures/{id}/fusions")
    public ResponseEntity<FtthFusionResponse> createFusion(
            @PathVariable UUID id,
            @Valid @RequestBody FtthFusionRequest request
    ) {
        request.setClosureId(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(fusionService.createFusion(request));
    }

    @DeleteMapping("/fusions/{id}")
    public ResponseEntity<Void> deleteFusion(@PathVariable UUID id) {
        fusionService.deleteFusion(id);
        return ResponseEntity.noContent().build();
    }

    // --- Caixas de Atendimento (CTO) ---
    @GetMapping("/ctos")
    public ResponseEntity<List<FtthCtoResponse>> getAllCtos() {
        return ResponseEntity.ok(topologyService.getAllCtos());
    }

    @GetMapping("/ctos/{id}")
    public ResponseEntity<FtthCtoResponse> getCtoById(@PathVariable UUID id) {
        return ResponseEntity.ok(topologyService.getCtoById(id));
    }

    @PostMapping("/ctos")
    public ResponseEntity<FtthCtoResponse> createCto(@Valid @RequestBody FtthCtoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(topologyService.createCto(request));
    }

    // --- Viabilidade de Vendas & Rastreamento Óptico ---
    @PostMapping("/feasibility")
    public ResponseEntity<FtthFeasibilityResponse> checkFeasibility(@Valid @RequestBody FtthFeasibilityRequest request) {
        return ResponseEntity.ok(topologyService.calculateFeasibility(request));
    }

    @GetMapping("/lightpath/{ctoPortId}")
    public ResponseEntity<FtthLightPathService.LightPathTraceResult> traceLightPath(@PathVariable UUID ctoPortId) {
        return ResponseEntity.ok(lightPathService.traceLightPathFromCtoPort(ctoPortId));
    }
}
