package br.dev.xb.isperp.controller.financial;

import br.dev.xb.isperp.api.contract.SentinelAuditApi;
import br.dev.xb.isperp.api.dto.SentinelAuditLogDto;
import br.dev.xb.isperp.mapper.FinancialDomainMapper;
import br.dev.xb.isperp.service.financial.SentinelWatchdogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"", "/api"})
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SentinelAuditController implements SentinelAuditApi {

    private final SentinelWatchdogService sentinelWatchdogService;
    private final FinancialDomainMapper financialDomainMapper;

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'CFO', 'DIRECTOR')")
    public ResponseEntity<List<SentinelAuditLogDto>> getActiveSentinelAlerts() {
        return ResponseEntity.ok(financialDomainMapper.toOpenApiSentinelAuditLogList(sentinelWatchdogService.getActiveAuditAlerts()));
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'CFO', 'DIRECTOR')")
    public ResponseEntity<List<SentinelAuditLogDto>> triggerSentinelSweep() {
        return ResponseEntity.ok(financialDomainMapper.toOpenApiSentinelAuditLogList(sentinelWatchdogService.triggerManualSweep()));
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'CFO', 'DIRECTOR')")
    public ResponseEntity<SentinelAuditLogDto> resolveSentinelAlert(UUID id) {
        return ResponseEntity.ok(financialDomainMapper.toOpenApiSentinelAuditLog(sentinelWatchdogService.resolveAuditAlert(id)));
    }
}
