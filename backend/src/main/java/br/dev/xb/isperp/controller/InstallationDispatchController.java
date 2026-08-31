package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.dto.InstallationMaterialDemandResponse;
import br.dev.xb.isperp.dto.TechnicianDispatchCandidateResponse;
import br.dev.xb.isperp.entity.WorkOrder;
import br.dev.xb.isperp.service.InstallationDemandService;
import br.dev.xb.isperp.service.TechnicianDispatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/dispatch/installations")
@RequiredArgsConstructor
@Tag(name = "Despacho de Instalações", description = "Endpoints para triagem de demandas FTTH, estoque em veículos e despacho de O.S.")
public class InstallationDispatchController {

    private final InstallationDemandService demandService;
    private final TechnicianDispatchService dispatchService;

    @GetMapping("/demands")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_ANALYST', 'SUPPORT_N2', 'FINANCIAL')")
    @Operation(summary = "Listar todas as demandas de instalação e dimensionamento FTTH")
    public ResponseEntity<List<InstallationMaterialDemandResponse>> listDemands() {
        return ResponseEntity.ok(demandService.listPendingDemands());
    }

    @GetMapping("/demands/{workOrderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_ANALYST', 'SUPPORT_N2', 'FINANCIAL')")
    @Operation(summary = "Obter detalhes da demanda e kit FTTH de uma O.S.")
    public ResponseEntity<InstallationMaterialDemandResponse> getDemand(@PathVariable UUID workOrderId) {
        return ResponseEntity.ok(demandService.getDemandByWorkOrder(workOrderId));
    }

    @GetMapping("/{workOrderId}/candidates")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_ANALYST', 'SUPPORT_N2')")
    @Operation(summary = "Listar técnicos candidatos ranqueados por estoque no veículo e proximidade GPS")
    public ResponseEntity<List<TechnicianDispatchCandidateResponse>> listCandidates(@PathVariable UUID workOrderId) {
        return ResponseEntity.ok(dispatchService.listCandidatesForWorkOrder(workOrderId));
    }

    @PostMapping("/{workOrderId}/dispatch")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_ANALYST', 'SUPPORT_N2')")
    @Operation(summary = "Despachar O.S. para um técnico com alocação automática de estoque do veículo")
    public ResponseEntity<WorkOrder> dispatchWorkOrder(
            @PathVariable UUID workOrderId,
            @RequestParam UUID technicianId) {
        return ResponseEntity.ok(dispatchService.dispatchWorkOrder(workOrderId, technicianId));
    }
}
