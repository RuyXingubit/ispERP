package br.dev.xb.isperp.service;

import br.dev.xb.isperp.client.GeoCepClient;
import br.dev.xb.isperp.entity.Customer;
import br.dev.xb.isperp.entity.ServiceRoute;
import br.dev.xb.isperp.entity.ServiceRouteStop;
import br.dev.xb.isperp.entity.WorkOrder;
import br.dev.xb.isperp.repository.CustomerRepository;
import br.dev.xb.isperp.repository.ServiceRouteRepository;
import br.dev.xb.isperp.repository.ServiceRouteStopRepository;
import br.dev.xb.isperp.repository.WorkOrderRepository;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class RouteOptimizationService {

    private final GeoCepClient geoCepClient;
    private final WorkOrderRepository workOrderRepository;
    private final CustomerRepository customerRepository;
    private final ServiceRouteRepository routeRepository;
    private final ServiceRouteStopRepository routeStopRepository;

    @Data
    @Builder
    public static class CreateRouteRequest {
        private UUID technicianUserId;
        private LocalDate routeDate;
        private BigDecimal originLatitude;
        private BigDecimal originLongitude;
        private List<UUID> workOrderIds;
    }

    public List<ServiceRoute> getRoutesByDate(LocalDate date) {
        return routeRepository.findByRouteDate(date);
    }

    public List<ServiceRouteStop> getStopsByRouteId(UUID routeId) {
        return routeStopRepository.findByRouteIdOrderBySequenceOrderAsc(routeId);
    }

    @Transactional
    public ServiceRoute optimizeAndCreateRoute(@NonNull CreateRouteRequest request) {
        String code = "ROTA-" + (request.getRouteDate() != null ? request.getRouteDate().toString().replaceAll("-", "") : "HOJE")
                + "-" + (System.currentTimeMillis() % 10000);

        log.info("Otimizando rota {} para o técnico {} com {} ordens de serviço", code, request.getTechnicianUserId(), request.getWorkOrderIds().size());

        // Ponto de Origem (ex: Depósito Central Altamira / Escritório)
        BigDecimal origLat = request.getOriginLatitude() != null ? request.getOriginLatitude() : new BigDecimal("-3.2033");
        BigDecimal origLon = request.getOriginLongitude() != null ? request.getOriginLongitude() : new BigDecimal("-52.2064");

        GeoCepClient.RouteWaypoint origin = GeoCepClient.RouteWaypoint.builder()
                .id("ORIGIN-DEPOT")
                .label("Depósito / Ponto de Partida")
                .latitude(origLat)
                .longitude(origLon)
                .build();

        List<GeoCepClient.RouteWaypoint> stops = new ArrayList<>();
        List<WorkOrder> workOrders = new ArrayList<>();

        for (UUID woId : request.getWorkOrderIds()) {
            workOrderRepository.findById(woId).ifPresent(wo -> {
                workOrders.add(wo);
                Customer c = customerRepository.findById(wo.getCustomerId()).orElse(null);
                BigDecimal lat = (c != null && c.getLatitude() != null) ? c.getLatitude() : origLat.add(new BigDecimal("0.005"));
                BigDecimal lon = (c != null && c.getLongitude() != null) ? c.getLongitude() : origLon.add(new BigDecimal("0.005"));

                stops.add(GeoCepClient.RouteWaypoint.builder()
                        .id(wo.getId().toString())
                        .label(c != null ? c.getName() : "Cliente O.S.")
                        .latitude(lat)
                        .longitude(lon)
                        .build());
            });
        }

        // Chama o cálculo de otimização da menor rota (TSP) via GeoCEP
        GeoCepClient.OptimizedRouteResult tspResult = geoCepClient.optimizeRoute(origin, stops);

        ServiceRoute route = ServiceRoute.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .code(code)
                .technicianUserId(request.getTechnicianUserId())
                .routeDate(request.getRouteDate() != null ? request.getRouteDate() : LocalDate.now())
                .totalDistanceKm(tspResult.getTotalDistanceKm())
                .estimatedDurationMinutes(tspResult.getEstimatedDurationMinutes())
                .status(ServiceRoute.RouteStatus.PLANNED)
                .build();

        ServiceRoute savedRoute = routeRepository.save(route);

        int seq = 1;
        for (GeoCepClient.RouteWaypoint waypoint : tspResult.getOrderedWaypoints()) {
            UUID woId = UUID.fromString(waypoint.getId());
            WorkOrder wo = workOrders.stream().filter(w -> w.getId().equals(woId)).findFirst().orElse(null);
            Customer c = wo != null ? customerRepository.findById(wo.getCustomerId()).orElse(null) : null;

            ServiceRouteStop stop = ServiceRouteStop.builder()
                    .id(UuidCreatorUtils.generateUuidV7())
                    .routeId(savedRoute.getId())
                    .workOrderId(woId)
                    .sequenceOrder(seq++)
                    .latitude(waypoint.getLatitude())
                    .longitude(waypoint.getLongitude())
                    .customerName(c != null ? c.getName() : "Cliente")
                    .address(c != null ? c.getAddress() : "Endereço da O.S.")
                    .completed(false)
                    .build();

            routeStopRepository.save(stop);
        }

        log.info("Rota {} gerada com sucesso: {} km, {} minutos estimados e {} paradas ordenadas",
                savedRoute.getCode(), savedRoute.getTotalDistanceKm(), savedRoute.getEstimatedDurationMinutes(), tspResult.getOrderedWaypoints().size());
        return savedRoute;
    }
}
