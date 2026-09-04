package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.dto.*;
import br.dev.xb.isperp.scheduler.RadiusLifecycleScheduler;
import br.dev.xb.isperp.service.RadiusLifecycleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/radius/lifecycle", "/api/radius/lifecycle"})
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Tag(name = "RADIUS Lifecycle & Auto-Corte", description = "Endpoints para gestão de políticas de inadimplência, auto-corte, desbloqueio e auditoria de ações PoD")
@SuppressWarnings("null")
public class RadiusLifecycleController {

    private final RadiusLifecycleService lifecycleService;
    private final RadiusLifecycleScheduler lifecycleScheduler;

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_N2', 'FINANCIAL')")
    @Operation(summary = "Resumo estatístico de assinantes, bloqueios e políticas de auto-corte")
    public ResponseEntity<RadiusLifecycleSummaryResponse> getSummary() {
        return ResponseEntity.ok(lifecycleService.getSummary());
    }

    @GetMapping("/policy")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_N2', 'FINANCIAL')")
    @Operation(summary = "Consulta configuração de políticas de tolerância e auto-corte")
    public ResponseEntity<RadiusPolicyConfigResponse> getPolicy() {
        return ResponseEntity.ok(lifecycleService.getPolicyConfigResponse());
    }

    @PutMapping("/policy")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_N2')")
    @Operation(summary = "Atualiza parâmetros e dias de tolerância do auto-corte")
    public ResponseEntity<RadiusPolicyConfigResponse> updatePolicy(@Valid @RequestBody RadiusPolicyConfigRequest request) {
        return ResponseEntity.ok(lifecycleService.updatePolicyConfig(request));
    }

    @GetMapping("/logs")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_N2', 'FINANCIAL')")
    @Operation(summary = "Logs paginados de auditoria de cortes, desbloqueios e pacotes PoD")
    public ResponseEntity<Page<RadiusLifecycleLogResponse>> getLogs(Pageable pageable) {
        return ResponseEntity.ok(lifecycleService.getLogs(pageable));
    }

    @PostMapping("/action")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_N2', 'FINANCIAL')")
    @Operation(summary = "Executa ação manual de bloqueio ou desbloqueio com envio de PoD")
    public ResponseEntity<RadiusManualActionResponse> executeManualAction(@Valid @RequestBody RadiusManualActionRequest request) {
        return ResponseEntity.ok(lifecycleService.executeManualAction(request));
    }

    @PostMapping("/run-autoblock")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_N2')")
    @Operation(summary = "Executa manualmente a rotina de varredura e auto-corte de inadimplentes")
    public ResponseEntity<Void> runAutoBlock() {
        lifecycleScheduler.processAutoBlockRoutine();
        return ResponseEntity.ok().build();
    }
}
