package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.client.GeoCepClient;
import br.dev.xb.isperp.entity.ServiceRoute;
import br.dev.xb.isperp.entity.ServiceRouteStop;
import br.dev.xb.isperp.service.RouteOptimizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/geocep")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@SuppressWarnings("null")
public class GeoCepController {

    private final GeoCepClient geoCepClient;
    private final RouteOptimizationService routeOptimizationService;

    @GetMapping("/cep/{cep}")
    public ResponseEntity<GeoCepClient.CepLookupResult> lookupCep(@PathVariable String cep) {
        return ResponseEntity.ok(geoCepClient.lookupCep(cep));
    }

    @PostMapping("/routes/optimize")
    public ResponseEntity<ServiceRoute> createOptimizedRoute(@RequestBody RouteOptimizationService.CreateRouteRequest request) {
        return ResponseEntity.ok(routeOptimizationService.optimizeAndCreateRoute(request));
    }

    @GetMapping("/routes")
    public ResponseEntity<List<ServiceRoute>> getRoutesByDate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(routeOptimizationService.getRoutesByDate(targetDate));
    }

    @GetMapping("/routes/{routeId}/stops")
    public ResponseEntity<List<ServiceRouteStop>> getStopsByRouteId(@PathVariable UUID routeId) {
        return ResponseEntity.ok(routeOptimizationService.getStopsByRouteId(routeId));
    }
}
