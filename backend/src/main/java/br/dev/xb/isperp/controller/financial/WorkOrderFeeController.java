package br.dev.xb.isperp.controller.financial;

import br.dev.xb.isperp.dto.financial.WorkOrderFeeAuditRequest;
import br.dev.xb.isperp.dto.financial.WorkOrderFeeDto;
import br.dev.xb.isperp.dto.financial.WorkOrderFeeWaiverRequest;
import br.dev.xb.isperp.service.financial.WorkOrderFeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/financial/work-orders")
@RequiredArgsConstructor
@Tag(name = "Taxas de O.S. & Esteira de Isenção", description = "Endpoints para tarifação de serviços técnicos, solicitação de isenção e auditoria gerencial")
public class WorkOrderFeeController {

    private final WorkOrderFeeService workOrderFeeService;

    @PostMapping("/{id}/assign-fee")
    @PreAuthorize("hasAnyRole('ADMIN', 'CFO', 'DIRECTOR', 'FINANCIAL', 'ATTENDANT')")
    @Operation(summary = "Atribui taxa padrão da tabela de serviços à Ordem de Serviço")
    public ResponseEntity<WorkOrderFeeDto> assignStandardFee(
            @PathVariable UUID id,
            @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(workOrderFeeService.assignStandardFee(id, amount));
    }

    @PostMapping("/waiver/request")
    @PreAuthorize("hasAnyRole('ADMIN', 'CFO', 'DIRECTOR', 'FINANCIAL', 'ATTENDANT')")
    @Operation(summary = "Atendente solicita isenção da taxa de serviço com justificativa de retenção comercial")
    public ResponseEntity<WorkOrderFeeDto> requestWaiver(
            @RequestHeader("X-User-Id") UUID attendantUserId,
            @Valid @RequestBody WorkOrderFeeWaiverRequest request) {
        return ResponseEntity.ok(workOrderFeeService.requestWaiver(attendantUserId, request));
    }

    @GetMapping("/waiver/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'CFO', 'DIRECTOR', 'FINANCIAL')")
    @Operation(summary = "Lista solicitações de isenção de taxas pendentes de aprovação gerencial")
    public ResponseEntity<List<WorkOrderFeeDto>> getPendingWaivers() {
        return ResponseEntity.ok(workOrderFeeService.getPendingWaiverAudits());
    }

    @PostMapping("/{id}/waiver/audit")
    @PreAuthorize("hasAnyRole('ADMIN', 'CFO', 'DIRECTOR', 'FINANCIAL')")
    @Operation(summary = "Gestor/CFO aprova ou rejeita isenção de taxa (dispara mensagem oficial anti-fraude ao cliente)")
    public ResponseEntity<WorkOrderFeeDto> auditWaiver(
            @RequestHeader("X-User-Id") UUID managerUserId,
            @PathVariable UUID id,
            @Valid @RequestBody WorkOrderFeeAuditRequest request) {
        return ResponseEntity.ok(workOrderFeeService.auditWaiver(managerUserId, id, request));
    }
}
