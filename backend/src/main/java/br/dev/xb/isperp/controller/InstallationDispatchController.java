package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.contract.InstallationDispatchApi;
import br.dev.xb.isperp.api.dto.InstallationMaterialDemandResponse;
import br.dev.xb.isperp.api.dto.MaterialDemandStatus;
import br.dev.xb.isperp.api.dto.TechnicianDispatchCandidateResponse;
import br.dev.xb.isperp.api.dto.WorkOrderResponse;
import br.dev.xb.isperp.entity.WorkOrder;
import br.dev.xb.isperp.mapper.WorkOrderMapper;
import br.dev.xb.isperp.service.InstallationDemandService;
import br.dev.xb.isperp.service.TechnicianDispatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"/dispatch/installations", "/api/dispatch/installations"})
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Despacho de Instalações", description = "Endpoints para triagem de demandas FTTH, estoque em veículos e despacho de O.S.")
@SuppressWarnings("null")
public class InstallationDispatchController implements InstallationDispatchApi {

    private final InstallationDemandService demandService;
    private final TechnicianDispatchService dispatchService;
    private final WorkOrderMapper workOrderMapper;

    @Override
    @GetMapping({"/demands", "/api/dispatch/installations/demands"})
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT', 'SUPPORT_ANALYST', 'SUPPORT_N2', 'FINANCIAL', 'SALES')")
    @Operation(summary = "Listar todas as demandas de instalação e dimensionamento FTTH")
    public ResponseEntity<List<InstallationMaterialDemandResponse>> listInstallationDemands() {
        return ResponseEntity.ok(demandService.listPendingDemands().stream()
                .map(this::toApiDemandResponse)
                .collect(Collectors.toList()));
    }

    @Override
    @GetMapping({"/demands/{workOrderId}", "/api/dispatch/installations/demands/{workOrderId}"})
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT', 'SUPPORT_ANALYST', 'SUPPORT_N2', 'FINANCIAL', 'SALES')")
    @Operation(summary = "Obter detalhes da demanda e kit FTTH de uma O.S.")
    public ResponseEntity<InstallationMaterialDemandResponse> getInstallationDemand(@PathVariable UUID workOrderId) {
        return ResponseEntity.ok(toApiDemandResponse(demandService.getDemandByWorkOrder(workOrderId)));
    }

    @Override
    @GetMapping({"/{workOrderId}/candidates", "/api/dispatch/installations/{workOrderId}/candidates"})
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT', 'SUPPORT_ANALYST', 'SUPPORT_N2')")
    @Operation(summary = "Listar técnicos candidatos ranqueados por estoque no veículo e proximidade GPS")
    public ResponseEntity<List<TechnicianDispatchCandidateResponse>> listDispatchCandidates(@PathVariable UUID workOrderId) {
        return ResponseEntity.ok(dispatchService.listCandidatesForWorkOrder(workOrderId).stream()
                .map(this::toApiCandidateResponse)
                .collect(Collectors.toList()));
    }

    @Override
    @PostMapping({"/{workOrderId}/dispatch", "/api/dispatch/installations/{workOrderId}/dispatch"})
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT', 'SUPPORT_ANALYST', 'SUPPORT_N2')")
    @Operation(summary = "Despachar O.S. para um técnico com alocação automática de estoque do veículo")
    public ResponseEntity<WorkOrderResponse> dispatchInstallationWorkOrder(
            @PathVariable UUID workOrderId,
            @RequestParam UUID technicianId) {
        WorkOrder wo = dispatchService.dispatchWorkOrder(workOrderId, technicianId);
        return ResponseEntity.ok(workOrderMapper.toResponse(wo));
    }

    @Override
    @PostMapping({"/demands/{workOrderId}/confirm-stock", "/api/dispatch/installations/demands/{workOrderId}/confirm-stock"})
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT', 'SUPPORT_ANALYST', 'SUPPORT_N2', 'FINANCIAL', 'SALES')")
    @Operation(summary = "Confirmar e reservar materiais no almoxarifado para a O.S.")
    public ResponseEntity<InstallationMaterialDemandResponse> confirmStockAllocation(
            @PathVariable UUID workOrderId,
            @RequestParam(required = false) UUID warehouseId) {
        return ResponseEntity.ok(toApiDemandResponse(demandService.confirmStockAllocation(workOrderId, warehouseId)));
    }

    private InstallationMaterialDemandResponse toApiDemandResponse(br.dev.xb.isperp.dto.InstallationMaterialDemandResponse res) {
        if (res == null) return null;
        InstallationMaterialDemandResponse api = new InstallationMaterialDemandResponse();
        api.setId(res.getId());
        api.setWorkOrderId(res.getWorkOrderId());
        api.setContractId(res.getContractId());
        api.setContractNumber(res.getContractNumber());
        api.setCustomerName(res.getCustomerName());
        api.setCustomerPhone(res.getCustomerPhone());
        api.setCustomerAddress(res.getCustomerAddress());
        api.setCustomerLatitude(res.getCustomerLatitude() != null ? res.getCustomerLatitude().doubleValue() : null);
        api.setCustomerLongitude(res.getCustomerLongitude() != null ? res.getCustomerLongitude().doubleValue() : null);
        api.setCtoId(res.getCtoId());
        api.setCtoName(res.getCtoName());
        api.setCtoLatitude(res.getCtoLatitude() != null ? res.getCtoLatitude().doubleValue() : null);
        api.setCtoLongitude(res.getCtoLongitude() != null ? res.getCtoLongitude().doubleValue() : null);
        api.setCtoPortNumber(res.getCtoPortNumber());
        api.setEstimatedDropMeters(res.getEstimatedDropMeters());
        api.setOnuModelRequired(res.getOnuModelRequired());
        api.setFastConnectorsCount(res.getFastConnectorsCount());
        api.setPtoRosetteCount(res.getPtoRosetteCount());
        if (res.getStatus() != null) {
            api.setStatus(MaterialDemandStatus.fromValue(res.getStatus().name()));
        }
        api.setAllocatedWarehouseId(res.getAllocatedWarehouseId());
        api.setAllocatedWarehouseName(res.getAllocatedWarehouseName());
        api.setAllocatedTechnicianName(res.getAllocatedTechnicianName());
        api.setWorkOrderType(res.getWorkOrderType());
        if (res.getCreatedAt() != null) {
            api.setCreatedAt(res.getCreatedAt().atZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime());
        }
        return api;
    }

    private TechnicianDispatchCandidateResponse toApiCandidateResponse(br.dev.xb.isperp.dto.TechnicianDispatchCandidateResponse res) {
        if (res == null) return null;
        TechnicianDispatchCandidateResponse api = new TechnicianDispatchCandidateResponse();
        api.setTechnicianId(res.getTechnicianId());
        api.setTechnicianName(res.getTechnicianName());
        api.setWarehouseId(res.getWarehouseId());
        api.setVehicleWarehouseName(res.getVehicleWarehouseName());
        api.setHasCompleteKit(res.getHasCompleteKit());
        api.setHasOnu(res.getHasOnu());
        api.setHasDropCable(res.getHasDropCable());
        api.setHasConnectors(res.getHasConnectors());
        api.setDropCableBalanceMeters(res.getDropCableBalanceMeters());
        api.setCurrentLatitude(res.getCurrentLatitude() != null ? res.getCurrentLatitude().doubleValue() : null);
        api.setCurrentLongitude(res.getCurrentLongitude() != null ? res.getCurrentLongitude().doubleValue() : null);
        api.setDistanceKmToCustomer(res.getDistanceKmToCustomer());
        api.setLastServiceAddress(res.getLastServiceAddress());
        api.setRecommendedScore(res.getRecommendedScore());
        return api;
    }
}
