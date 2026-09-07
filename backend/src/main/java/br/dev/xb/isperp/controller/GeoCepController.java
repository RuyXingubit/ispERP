package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.contract.GeoCepApi;
import br.dev.xb.isperp.api.dto.CepLookupResult;
import br.dev.xb.isperp.api.dto.ContributeCoordinateRequest;
import br.dev.xb.isperp.api.dto.ContributeCoordinateResponse;
import br.dev.xb.isperp.api.dto.CreateRouteRequest;
import br.dev.xb.isperp.api.dto.RouteStatus;
import br.dev.xb.isperp.api.dto.ServiceRouteResponse;
import br.dev.xb.isperp.api.dto.ServiceRouteStopResponse;
import br.dev.xb.isperp.client.GeoCepClient;
import br.dev.xb.isperp.entity.ServiceRoute;
import br.dev.xb.isperp.entity.ServiceRouteStop;
import br.dev.xb.isperp.service.RouteOptimizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"/geocep", "/api/geocep"})
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@SuppressWarnings("null")
public class GeoCepController implements GeoCepApi {

    private final GeoCepClient geoCepClient;
    private final RouteOptimizationService routeOptimizationService;

    @Override
    @GetMapping("/cep/{cep}")
    public ResponseEntity<CepLookupResult> lookupCep(
            @PathVariable String cep,
            @RequestParam(required = false) String numero) {
        return ResponseEntity.ok(toApiCepResult(geoCepClient.lookupCep(cep, numero)));
    }

    @Override
    @GetMapping("/search")
    public ResponseEntity<List<CepLookupResult>> searchAddresses(@RequestParam String q) {
        return ResponseEntity.ok(geoCepClient.searchAddresses(q).stream()
                .map(this::toApiCepResult)
                .collect(Collectors.toList()));
    }

    @Override
    @GetMapping("/reverse")
    public ResponseEntity<CepLookupResult> reverseGeocode(
            @RequestParam Double lat,
            @RequestParam Double lon) {
        BigDecimal bLat = lat != null ? BigDecimal.valueOf(lat) : null;
        BigDecimal bLon = lon != null ? BigDecimal.valueOf(lon) : null;
        return ResponseEntity.ok(toApiCepResult(geoCepClient.reverseGeocode(bLat, bLon)));
    }

    @Override
    @PostMapping("/routes/optimize")
    public ResponseEntity<ServiceRouteResponse> createOptimizedRoute(@RequestBody CreateRouteRequest request) {
        RouteOptimizationService.CreateRouteRequest internalReq = RouteOptimizationService.CreateRouteRequest.builder()
                .technicianUserId(request.getTechnicianUserId())
                .routeDate(request.getRouteDate())
                .originLatitude(request.getOriginLatitude() != null ? BigDecimal.valueOf(request.getOriginLatitude()) : null)
                .originLongitude(request.getOriginLongitude() != null ? BigDecimal.valueOf(request.getOriginLongitude()) : null)
                .workOrderIds(request.getWorkOrderIds())
                .build();
        ServiceRoute route = routeOptimizationService.optimizeAndCreateRoute(internalReq);
        return ResponseEntity.ok(toApiServiceRoute(route));
    }

    @Override
    @GetMapping("/routes")
    public ResponseEntity<List<ServiceRouteResponse>> getRoutesByDate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(routeOptimizationService.getRoutesByDate(targetDate).stream()
                .map(this::toApiServiceRoute)
                .collect(Collectors.toList()));
    }

    @Override
    @GetMapping("/routes/{routeId}/stops")
    public ResponseEntity<List<ServiceRouteStopResponse>> getStopsByRouteId(@PathVariable UUID routeId) {
        return ResponseEntity.ok(routeOptimizationService.getStopsByRouteId(routeId).stream()
                .map(this::toApiRouteStop)
                .collect(Collectors.toList()));
    }

    @Override
    @PostMapping("/contribute")
    public ResponseEntity<ContributeCoordinateResponse> contributeCoordinate(
            @RequestBody ContributeCoordinateRequest request) {
        GeoCepClient.ContributeCoordinateRequest internalReq = GeoCepClient.ContributeCoordinateRequest.builder()
                .cep(request.getCep())
                .numero(request.getNumero())
                .latitude(request.getLatitude() != null ? BigDecimal.valueOf(request.getLatitude()) : null)
                .longitude(request.getLongitude() != null ? BigDecimal.valueOf(request.getLongitude()) : null)
                .precisaoGpsMetros(request.getPrecisaoGpsMetros() != null ? BigDecimal.valueOf(request.getPrecisaoGpsMetros()) : null)
                .build();
        GeoCepClient.ContributeCoordinateResponse internalResp = geoCepClient.contributeCoordinate(internalReq);
        ContributeCoordinateResponse resp = new ContributeCoordinateResponse();
        resp.setStatus(internalResp.getStatus());
        resp.setData(internalResp.getData());
        return ResponseEntity.ok(resp);
    }

    private CepLookupResult toApiCepResult(GeoCepClient.CepLookupResult res) {
        if (res == null) return null;
        CepLookupResult apiRes = new CepLookupResult();
        apiRes.setCep(res.getCep());
        apiRes.setLogradouro(res.getLogradouro());
        apiRes.setComplemento(res.getComplemento());
        apiRes.setBairro(res.getBairro());
        apiRes.setLocalidade(res.getLocalidade());
        apiRes.setUf(res.getUf());
        apiRes.setIbge(res.getIbge());
        apiRes.setLatitude(res.getLatitude() != null ? res.getLatitude().doubleValue() : null);
        apiRes.setLongitude(res.getLongitude() != null ? res.getLongitude().doubleValue() : null);
        return apiRes;
    }

    private ServiceRouteResponse toApiServiceRoute(ServiceRoute route) {
        if (route == null) return null;
        ServiceRouteResponse res = new ServiceRouteResponse();
        res.setId(route.getId());
        res.setCode(route.getCode());
        res.setTechnicianUserId(route.getTechnicianUserId());
        res.setRouteDate(route.getRouteDate());
        res.setTotalDistanceKm(route.getTotalDistanceKm() != null ? route.getTotalDistanceKm().doubleValue() : null);
        res.setEstimatedDurationMinutes(route.getEstimatedDurationMinutes());
        if (route.getStatus() != null) {
            res.setStatus(RouteStatus.fromValue(route.getStatus().name()));
        }
        if (route.getCreatedAt() != null) {
            res.setCreatedAt(route.getCreatedAt().atOffset(ZoneOffset.UTC));
        }
        if (route.getUpdatedAt() != null) {
            res.setUpdatedAt(route.getUpdatedAt().atOffset(ZoneOffset.UTC));
        }
        return res;
    }

    private ServiceRouteStopResponse toApiRouteStop(ServiceRouteStop stop) {
        if (stop == null) return null;
        ServiceRouteStopResponse res = new ServiceRouteStopResponse();
        res.setId(stop.getId());
        res.setRouteId(stop.getRouteId());
        res.setWorkOrderId(stop.getWorkOrderId());
        res.setSequenceOrder(stop.getSequenceOrder());
        res.setLatitude(stop.getLatitude() != null ? stop.getLatitude().doubleValue() : null);
        res.setLongitude(stop.getLongitude() != null ? stop.getLongitude().doubleValue() : null);
        res.setCustomerName(stop.getCustomerName());
        res.setAddress(stop.getAddress());
        res.setCompleted(stop.getCompleted());
        return res;
    }
}
