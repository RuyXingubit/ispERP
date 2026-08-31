package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.TechnicianDispatchCandidateResponse;
import br.dev.xb.isperp.entity.*;
import br.dev.xb.isperp.event.GenericDomainEvent;
import br.dev.xb.isperp.repository.*;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class TechnicianDispatchService {

    private final UserRepository userRepository;
    private final WarehouseRepository warehouseRepository;
    private final SerializedAssetRepository serializedAssetRepository;
    private final WorkOrderRepository workOrderRepository;
    private final ContractRepository contractRepository;
    private final CustomerRepository customerRepository;
    private final InstallationMaterialDemandRepository demandRepository;
    private final InstallationDemandService demandService;
    private final DomainEventPublisher domainEventPublisher;

    @Transactional(readOnly = true)
    public List<TechnicianDispatchCandidateResponse> listCandidatesForWorkOrder(UUID workOrderId) {
        WorkOrder workOrder = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new NoSuchElementException("O.S. não encontrada: " + workOrderId));

        Contract contract = contractRepository.findById(workOrder.getContractId())
                .orElseThrow(() -> new NoSuchElementException("Contrato não encontrado: " + workOrder.getContractId()));

        Customer customer = customerRepository.findById(contract.getCustomerId())
                .orElseThrow(() -> new NoSuchElementException("Cliente não encontrado: " + contract.getCustomerId()));

        InstallationMaterialDemand demand = demandRepository.findByWorkOrderId(workOrderId)
                .orElseGet(() -> demandService.generateDemandForWorkOrder(workOrderId));

        List<User> technicians = userRepository.findByRole(UserRole.TECHNICIAN);
        if (technicians.isEmpty()) {
            technicians = userRepository.findAll(); // Fallback se não houver usuários com role restrita
        }

        List<TechnicianDispatchCandidateResponse> candidates = new ArrayList<>();

        for (User tech : technicians) {
            Optional<Warehouse> vehicleWarehouse = warehouseRepository.findByResponsibleUserId(tech.getId());
            UUID wId = vehicleWarehouse.map(Warehouse::getId).orElse(null);
            String wName = vehicleWarehouse.map(Warehouse::getName).orElse("Veículo " + tech.getName());

            // Avalia ONUs em custódia no veículo
            long onuCount = 0;
            if (wId != null) {
                onuCount = serializedAssetRepository.findByCurrentWarehouseIdAndStatus(wId, SerializedAsset.AssetStatus.DISPONIVEL_DEPOSITO).size();
            } else {
                onuCount = 3; // Padrão para simulação de técnicos ativos
            }

            boolean hasOnu = onuCount > 0;
            int dropBalance = 300; // Saldo estimado em metros na bobina do veículo
            boolean hasDropCable = dropBalance >= demand.getEstimatedDropMeters();
            boolean hasConnectors = true;
            boolean hasCompleteKit = hasOnu && hasDropCable && hasConnectors;

            // Posição do técnico (última O.S. ou padrão Belém/Pará)
            BigDecimal techLat = BigDecimal.valueOf(-1.4558);
            BigDecimal techLon = BigDecimal.valueOf(-48.4902);
            String lastServiceAddress = "Centro Operacional ISP";

            // Se o cliente tiver coordenadas, calcula distância
            Double distKm = 2.5;
            if (customer.getLatitude() != null && customer.getLongitude() != null) {
                double dMeters = calculateHaversineDistanceMeters(
                        techLat.doubleValue(), techLon.doubleValue(),
                        customer.getLatitude().doubleValue(), customer.getLongitude().doubleValue()
                );
                distKm = Math.round((dMeters / 1000.0) * 10.0) / 10.0;
            }

            // Cálculo do score (Kit completo + Proximidade)
            double score = (hasCompleteKit ? 60.0 : 20.0) + Math.max(0.0, 40.0 - (distKm * 2.5));

            candidates.add(TechnicianDispatchCandidateResponse.builder()
                    .technicianId(tech.getId())
                    .technicianName(tech.getName())
                    .warehouseId(wId)
                    .vehicleWarehouseName(wName)
                    .hasCompleteKit(hasCompleteKit)
                    .hasOnu(hasOnu)
                    .hasDropCable(hasDropCable)
                    .hasConnectors(hasConnectors)
                    .dropCableBalanceMeters(dropBalance)
                    .currentLatitude(techLat)
                    .currentLongitude(techLon)
                    .distanceKmToCustomer(distKm)
                    .lastServiceAddress(lastServiceAddress)
                    .recommendedScore(Math.round(score * 10.0) / 10.0)
                    .build());
        }

        candidates.sort(Comparator.comparing(TechnicianDispatchCandidateResponse::getRecommendedScore).reversed());
        return candidates;
    }

    @Transactional
    public WorkOrder dispatchWorkOrder(UUID workOrderId, UUID technicianId) {
        WorkOrder workOrder = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new NoSuchElementException("O.S. não encontrada: " + workOrderId));

        User technician = userRepository.findById(technicianId)
                .orElseThrow(() -> new NoSuchElementException("Técnico não encontrado: " + technicianId));

        Optional<Warehouse> vehicleWarehouse = warehouseRepository.findByResponsibleUserId(technicianId);

        workOrder.setTechnicianName(technician.getName());
        workOrder.setStatus(WorkOrder.WorkOrderStatus.SCHEDULED);
        workOrder.setScheduledDate(LocalDate.now());
        workOrder.setScheduledPeriod("IMEDIATO");
        if (vehicleWarehouse.isPresent()) {
            workOrder.setAllocatedWarehouseId(vehicleWarehouse.get().getId());
        }

        WorkOrder savedWo = workOrderRepository.save(workOrder);

        // Aloca os materiais na demanda
        InstallationMaterialDemand demand = demandRepository.findByWorkOrderId(workOrderId)
                .orElseGet(() -> demandService.generateDemandForWorkOrder(workOrderId));

        demand.setStatus(MaterialDemandStatus.ALLOCATED_VEHICLE);
        demand.setAllocatedTechnicianName(technician.getName());
        if (vehicleWarehouse.isPresent()) {
            demand.setAllocatedWarehouseId(vehicleWarehouse.get().getId());
        }
        demandRepository.save(demand);

        // Emite evento WORK_ORDER_DISPATCHED
        Map<String, Object> payload = new HashMap<>();
        payload.put("workOrderId", savedWo.getId().toString());
        payload.put("contractId", savedWo.getContractId().toString());
        payload.put("customerId", savedWo.getCustomerId().toString());
        payload.put("technicianId", technician.getId().toString());
        payload.put("technicianName", technician.getName());
        payload.put("allocatedWarehouseId", demand.getAllocatedWarehouseId() != null ? demand.getAllocatedWarehouseId().toString() : null);
        payload.put("estimatedDropMeters", demand.getEstimatedDropMeters());

        GenericDomainEvent event = GenericDomainEvent.builder()
                .eventId(UuidCreatorUtils.generateUuidV7())
                .eventType("WORK_ORDER_DISPATCHED")
                .aggregateType("WorkOrder")
                .aggregateId(savedWo.getId().toString())
                .payload(payload)
                .build();

        domainEventPublisher.publish(event);
        log.info("O.S. {} despachada com sucesso para o técnico {}", savedWo.getId(), technician.getName());
        return savedWo;
    }

    private double calculateHaversineDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
