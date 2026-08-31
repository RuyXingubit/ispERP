package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.dto.OltUnprovisionedOnuResponse;
import br.dev.xb.isperp.dto.TechnicianExecutionCompleteRequest;
import br.dev.xb.isperp.entity.WorkOrder;
import br.dev.xb.isperp.service.TechnicianFieldExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/technician/execution")
@RequiredArgsConstructor
@Tag(name = "Execução Técnica de Campo", description = "Endpoints para Auto-Discovery OLT, provisionamento 1-clique, verificação RADIUS e conclusão de O.S.")
public class TechnicianFieldExecutionController {

    private final TechnicianFieldExecutionService fieldExecutionService;

    @GetMapping("/{workOrderId}/unprovisioned-onus")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECHNICIAN', 'SUPPORT_N2', 'SUPPORT_ANALYST')")
    @Operation(summary = "Listar ONUs descobertas automaticamente (Auto-Find) na porta PON da OLT")
    public ResponseEntity<List<OltUnprovisionedOnuResponse>> listUnprovisionedOnus(@PathVariable UUID workOrderId) {
        return ResponseEntity.ok(fieldExecutionService.listUnprovisionedOnus(workOrderId));
    }

    @PostMapping("/{workOrderId}/provision")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECHNICIAN', 'SUPPORT_N2', 'SUPPORT_ANALYST')")
    @Operation(summary = "Provisionar ONU selecionada com VLAN e credenciais PPPoE no FreeRADIUS")
    public ResponseEntity<Map<String, Object>> provisionOnu(
            @PathVariable UUID workOrderId,
            @RequestParam String onuSerial,
            @RequestParam(required = false, defaultValue = "100") Integer vlanId,
            @RequestParam(required = false) String pppoeUsername,
            @RequestParam(required = false) String pppoePassword) {
        return ResponseEntity.ok(fieldExecutionService.provisionOnu(workOrderId, onuSerial, vlanId, pppoeUsername, pppoePassword));
    }

    @GetMapping("/{workOrderId}/radius-status")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECHNICIAN', 'SUPPORT_N2', 'SUPPORT_ANALYST')")
    @Operation(summary = "Verificar em tempo real se a sessão PPPoE do assinante autenticou no FreeRADIUS")
    public ResponseEntity<Map<String, Object>> checkRadiusStatus(@PathVariable UUID workOrderId) {
        return ResponseEntity.ok(fieldExecutionService.checkRadiusSessionStatus(workOrderId));
    }

    @PostMapping("/{workOrderId}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECHNICIAN', 'SUPPORT_N2', 'SUPPORT_ANALYST')")
    @Operation(summary = "Concluir O.S. com evidências fotográficas, potência dBm, assinatura digital e ativação do cliente")
    public ResponseEntity<WorkOrder> completeInstallation(
            @PathVariable UUID workOrderId,
            @Valid @RequestBody TechnicianExecutionCompleteRequest request) {
        return ResponseEntity.ok(fieldExecutionService.completeInstallation(workOrderId, request));
    }
}
