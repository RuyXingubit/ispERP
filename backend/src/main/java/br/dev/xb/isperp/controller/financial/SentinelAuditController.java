package br.dev.xb.isperp.controller.financial;

import br.dev.xb.isperp.dto.financial.SentinelAuditLogDto;
import br.dev.xb.isperp.service.financial.SentinelWatchdogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/financial/sentinel")
@RequiredArgsConstructor
@Tag(name = "Sentinela IA (Auditoria Forense)", description = "Varredura automatizada contra desvios de conduta, caixa 2 e retenção indevida de valores")
public class SentinelAuditController {

    private final SentinelWatchdogService sentinelWatchdogService;

    @GetMapping("/alerts")
    @PreAuthorize("hasAnyRole('ADMIN', 'CFO', 'DIRECTOR')")
    @Operation(summary = "Lista todos os alertas de auditoria pericial pendentes de resolução")
    public ResponseEntity<List<SentinelAuditLogDto>> getActiveAlerts() {
        return ResponseEntity.ok(sentinelWatchdogService.getActiveAuditAlerts());
    }

    @PostMapping("/sweep")
    @PreAuthorize("hasAnyRole('ADMIN', 'CFO', 'DIRECTOR')")
    @Operation(summary = "Dispara varredura pericial manual no sistema")
    public ResponseEntity<List<SentinelAuditLogDto>> triggerSweep() {
        return ResponseEntity.ok(sentinelWatchdogService.triggerManualSweep());
    }

    @PostMapping("/alerts/{id}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN', 'CFO', 'DIRECTOR')")
    @Operation(summary = "Marca um alerta pericial como auditado e resolvido pela diretoria")
    public ResponseEntity<SentinelAuditLogDto> resolveAlert(@PathVariable UUID id) {
        return ResponseEntity.ok(sentinelWatchdogService.resolveAuditAlert(id));
    }
}
