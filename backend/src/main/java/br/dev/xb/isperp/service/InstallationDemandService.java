package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.InstallationMaterialDemandResponse;
import br.dev.xb.isperp.entity.*;
import br.dev.xb.isperp.mapper.InstallationMaterialDemandMapper;
import br.dev.xb.isperp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class InstallationDemandService {

    private final InstallationMaterialDemandRepository demandRepository;
    private final WorkOrderRepository workOrderRepository;
    private final ContractRepository contractRepository;
    private final CustomerRepository customerRepository;
    private final PlanRepository planRepository;
    private final FtthCtoRepository ctoRepository;
    private final WarehouseRepository warehouseRepository;
    private final InstallationMaterialDemandMapper demandMapper;

    /**
     * Gera a demanda de materiais FTTH e metragem do drop para uma Ordem de Serviço de Instalação.
     */
    @Transactional
    public InstallationMaterialDemand generateDemandForWorkOrder(UUID workOrderId) {
        WorkOrder workOrder = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new NoSuchElementException("Ordem de Serviço não encontrada: " + workOrderId));

        Contract contract = contractRepository.findById(workOrder.getContractId())
                .orElseThrow(() -> new NoSuchElementException("Contrato não encontrado: " + workOrder.getContractId()));

        Customer customer = customerRepository.findById(workOrder.getCustomerId())
                .orElseThrow(() -> new NoSuchElementException("Cliente não encontrado: " + workOrder.getCustomerId()));

        Plan plan = planRepository.findById(contract.getPlanId()).orElse(null);

        // 1. Identifica a CTO mais próxima ou já vinculada
        FtthCto cto = null;
        if (contract.getCtoId() != null) {
            cto = ctoRepository.findById(contract.getCtoId()).orElse(null);
        }
        if (cto == null && customer.getLatitude() != null && customer.getLongitude() != null) {
            cto = findNearestCto(customer.getLatitude().doubleValue(), customer.getLongitude().doubleValue());
        }
        if (cto == null) {
            cto = ctoRepository.findAll().stream().findFirst().orElse(null);
        }

        // 2. Calcula metragem do drop óptico (distância + 20% folga técnica)
        int estimatedDropMeters = 50; // Padrão mínimo
        if (cto != null && cto.getLatitude() != null && cto.getLongitude() != null &&
                customer.getLatitude() != null && customer.getLongitude() != null) {
            double distMeters = calculateHaversineDistanceMeters(
                    cto.getLatitude().doubleValue(), cto.getLongitude().doubleValue(),
                    customer.getLatitude().doubleValue(), customer.getLongitude().doubleValue()
            );
            estimatedDropMeters = Math.max(30, (int) Math.ceil(distMeters * 1.20));
        }

        // 3. Define modelo de ONU pelo plano
        String onuModel = "ONT Wi-Fi Dual-Band GPON Gigabit";
        if (plan != null && plan.getDownloadSpeed() != null && plan.getDownloadSpeed() >= 1000) {
            onuModel = "ONT Wi-Fi 6 AX3000 XGS-PON / GPON 2.5G";
        }

        // Atualiza vínculo no contrato e na O.S.
        if (cto != null) {
            contract.setCtoId(cto.getId());
            workOrder.setCtoId(cto.getId());
            if (contract.getCtoPortNumber() != null) {
                workOrder.setCtoPortNumber(contract.getCtoPortNumber());
            } else {
                contract.setCtoPortNumber(1);
                workOrder.setCtoPortNumber(1);
            }
            contractRepository.save(contract);
            workOrderRepository.save(workOrder);
        }

        InstallationMaterialDemand demand = demandRepository.findByWorkOrderId(workOrderId)
                .orElseGet(() -> InstallationMaterialDemand.builder()
                        .workOrderId(workOrderId)
                        .contractId(contract.getId())
                        .build());

        demand.setCtoId(cto != null ? cto.getId() : null);
        demand.setCtoPortNumber(contract.getCtoPortNumber());
        demand.setEstimatedDropMeters(estimatedDropMeters);
        demand.setOnuModelRequired(onuModel);
        demand.setFastConnectorsCount(2);
        demand.setPtoRosetteCount(1);
        demand.setStatus(MaterialDemandStatus.PENDING_ALLOCATION);

        InstallationMaterialDemand saved = demandRepository.save(demand);
        log.info("Demanda FTTH gerada para O.S. {}: CTO={}, Drop={}m, ONU={}",
                workOrderId, cto != null ? cto.getName() : "N/A", estimatedDropMeters, onuModel);
        return saved;
    }

    @Transactional
    public List<InstallationMaterialDemandResponse> listPendingDemands() {
        List<WorkOrder> pendingOrders = workOrderRepository.findAll().stream()
                .filter(wo -> wo.getType() == WorkOrder.WorkOrderType.INSTALACAO)
                .toList();

        for (WorkOrder wo : pendingOrders) {
            if (demandRepository.findByWorkOrderId(wo.getId()).isEmpty()) {
                try {
                    generateDemandForWorkOrder(wo.getId());
                } catch (Exception e) {
                    log.warn("Não foi possível auto-gerar demanda para O.S. {}: {}", wo.getId(), e.getMessage());
                }
            }
        }

        List<InstallationMaterialDemand> demands = demandRepository.findAll();
        List<InstallationMaterialDemandResponse> responses = new ArrayList<>();
        for (InstallationMaterialDemand d : demands) {
            responses.add(enrichResponse(d));
        }
        return responses;
    }

    @Transactional(readOnly = true)
    public InstallationMaterialDemandResponse getDemandByWorkOrder(UUID workOrderId) {
        InstallationMaterialDemand demand = demandRepository.findByWorkOrderId(workOrderId)
                .orElseGet(() -> generateDemandForWorkOrder(workOrderId));
        return enrichResponse(demand);
    }

    private InstallationMaterialDemandResponse enrichResponse(InstallationMaterialDemand demand) {
        InstallationMaterialDemandResponse resp = demandMapper.toResponse(demand);

        contractRepository.findById(demand.getContractId()).ifPresent(c -> {
            resp.setContractNumber(c.getContractNumber());
            customerRepository.findById(c.getCustomerId()).ifPresent(cust -> {
                resp.setCustomerName(cust.getName());
                resp.setCustomerPhone(cust.getPhone());
                resp.setCustomerAddress(c.getInstallationAddress());
                resp.setCustomerLatitude(cust.getLatitude());
                resp.setCustomerLongitude(cust.getLongitude());
            });
        });

        if (demand.getCtoId() != null) {
            ctoRepository.findById(demand.getCtoId()).ifPresent(cto -> {
                resp.setCtoName(cto.getName());
                resp.setCtoLatitude(cto.getLatitude());
                resp.setCtoLongitude(cto.getLongitude());
            });
        }

        if (demand.getAllocatedWarehouseId() != null) {
            warehouseRepository.findById(demand.getAllocatedWarehouseId()).ifPresent(w -> {
                resp.setAllocatedWarehouseName(w.getName());
            });
        }

        return resp;
    }

    private @Nullable FtthCto findNearestCto(double lat, double lon) {
        List<FtthCto> ctos = ctoRepository.findAll();
        FtthCto nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (FtthCto cto : ctos) {
            if (cto.getLatitude() != null && cto.getLongitude() != null) {
                double d = calculateHaversineDistanceMeters(lat, lon, cto.getLatitude().doubleValue(), cto.getLongitude().doubleValue());
                if (d < minDistance) {
                    minDistance = d;
                    nearest = cto;
                }
            }
        }
        return nearest;
    }

    private double calculateHaversineDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Raio da Terra em metros
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
