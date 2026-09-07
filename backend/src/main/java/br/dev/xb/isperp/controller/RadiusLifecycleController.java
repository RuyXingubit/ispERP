package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.contract.RadiusLifecycleApi;
import br.dev.xb.isperp.api.dto.*;
import br.dev.xb.isperp.scheduler.RadiusLifecycleScheduler;
import br.dev.xb.isperp.service.RadiusLifecycleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"", "/api"})
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Tag(name = "RADIUS Lifecycle & Auto-Corte", description = "Endpoints para gestão de políticas de inadimplência, auto-corte, desbloqueio e auditoria de ações PoD")
@SuppressWarnings("null")
public class RadiusLifecycleController implements RadiusLifecycleApi {

    private final RadiusLifecycleService lifecycleService;
    private final RadiusLifecycleScheduler lifecycleScheduler;

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_N2', 'FINANCIAL')")
    public ResponseEntity<RadiusLifecycleSummaryResponse> getRadiusLifecycleSummary() {
        return ResponseEntity.ok(toApi(lifecycleService.getSummary()));
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_N2', 'FINANCIAL')")
    public ResponseEntity<RadiusPolicyConfigResponse> getRadiusPolicy() {
        return ResponseEntity.ok(toApi(lifecycleService.getPolicyConfigResponse()));
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_N2')")
    public ResponseEntity<RadiusPolicyConfigResponse> updateRadiusPolicy(RadiusPolicyConfigRequest request) {
        return ResponseEntity.ok(toApi(lifecycleService.updatePolicyConfig(fromApi(request))));
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_N2', 'FINANCIAL')")
    public ResponseEntity<RadiusLifecycleLogsPageResponse> getRadiusLifecycleLogs(
            Integer page,
            Integer size) {
        Pageable pageable = PageRequest.of(page != null ? page : 0, size != null ? size : 20);
        return ResponseEntity.ok(toPageResponse(lifecycleService.getLogs(pageable)));
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_N2', 'FINANCIAL')")
    public ResponseEntity<RadiusManualActionResponse> executeRadiusManualAction(RadiusManualActionRequest request) {
        return ResponseEntity.ok(toApi(lifecycleService.executeManualAction(fromApi(request))));
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_N2')")
    public ResponseEntity<Void> runRadiusAutoBlock() {
        lifecycleScheduler.processAutoBlockRoutine();
        return ResponseEntity.ok().build();
    }

    // Métodos auxiliares para compatibilidade com assinaturas internas legadas
    public ResponseEntity<RadiusLifecycleSummaryResponse> getSummary() {
        return getRadiusLifecycleSummary();
    }

    public ResponseEntity<RadiusPolicyConfigResponse> getPolicy() {
        return getRadiusPolicy();
    }

    public ResponseEntity<RadiusPolicyConfigResponse> updatePolicy(br.dev.xb.isperp.dto.RadiusPolicyConfigRequest request) {
        return ResponseEntity.ok(toApi(lifecycleService.updatePolicyConfig(request)));
    }

    public ResponseEntity<Page<br.dev.xb.isperp.dto.RadiusLifecycleLogResponse>> getLogs(Pageable pageable) {
        return ResponseEntity.ok(lifecycleService.getLogs(pageable));
    }

    public ResponseEntity<RadiusManualActionResponse> executeManualAction(br.dev.xb.isperp.dto.RadiusManualActionRequest request) {
        return ResponseEntity.ok(toApi(lifecycleService.executeManualAction(request)));
    }

    public ResponseEntity<Void> runAutoBlock() {
        return runRadiusAutoBlock();
    }

    private RadiusLifecycleSummaryResponse toApi(br.dev.xb.isperp.dto.RadiusLifecycleSummaryResponse s) {
        if (s == null) return null;
        return new RadiusLifecycleSummaryResponse(
                (long) s.getTotalPppoeUsers(),
                (long) s.getTotalActiveUsers(),
                (long) s.getTotalBlockedUsers(),
                (long) s.getTotalTrustUnblocked(),
                (long) s.getTodayAutoBlocksCount(),
                (long) s.getTodayUnblocksCount(),
                s.getToleranceDays(),
                s.isAutoBlockEnabled()
        );
    }

    private RadiusPolicyConfigResponse toApi(br.dev.xb.isperp.dto.RadiusPolicyConfigResponse p) {
        if (p == null) return null;
        RadiusPolicyConfigResponse resp = new RadiusPolicyConfigResponse();
        resp.setId(p.getId());
        resp.setAutoBlockEnabled(p.isAutoBlockEnabled());
        resp.setToleranceDays(p.getToleranceDays());
        if (p.getBlockMode() != null) {
            resp.setBlockMode(RadiusBlockMode.fromValue(p.getBlockMode().name()));
        }
        resp.setReducedDownloadKbps(p.getReducedDownloadKbps());
        resp.setReducedUploadKbps(p.getReducedUploadKbps());
        resp.setUnblockOnPayment(p.isUnblockOnPayment());
        resp.setSendPodOnBlock(p.isSendPodOnBlock());
        resp.setSendPodOnUnblock(p.isSendPodOnUnblock());
        resp.setBlockStartHour(p.getBlockStartHour());
        resp.setBlockEndHour(p.getBlockEndHour());
        resp.setAllowBlockOnFriday(p.isAllowBlockOnFriday());
        resp.setProtectEveOfHolidays(p.isProtectEveOfHolidays());
        resp.setCreatedAt(p.getCreatedAt());
        resp.setUpdatedAt(p.getUpdatedAt());
        return resp;
    }

    private br.dev.xb.isperp.dto.RadiusPolicyConfigRequest fromApi(RadiusPolicyConfigRequest req) {
        if (req == null) return null;
        return br.dev.xb.isperp.dto.RadiusPolicyConfigRequest.builder()
                .autoBlockEnabled(req.getAutoBlockEnabled())
                .toleranceDays(req.getToleranceDays())
                .blockMode(req.getBlockMode() != null ? br.dev.xb.isperp.radius.RadiusBlockMode.valueOf(req.getBlockMode().getValue()) : br.dev.xb.isperp.radius.RadiusBlockMode.CAPTIVE_PORTAL)
                .reducedDownloadKbps(req.getReducedDownloadKbps())
                .reducedUploadKbps(req.getReducedUploadKbps())
                .unblockOnPayment(req.getUnblockOnPayment())
                .sendPodOnBlock(req.getSendPodOnBlock())
                .sendPodOnUnblock(req.getSendPodOnUnblock())
                .blockStartHour(req.getBlockStartHour())
                .blockEndHour(req.getBlockEndHour())
                .allowBlockOnFriday(req.getAllowBlockOnFriday())
                .protectEveOfHolidays(req.getProtectEveOfHolidays())
                .build();
    }

    private RadiusManualActionResponse toApi(br.dev.xb.isperp.dto.RadiusManualActionResponse r) {
        if (r == null) return null;
        return new RadiusManualActionResponse(
                r.getContractId(),
                r.getUsername(),
                r.getActionApplied(),
                r.isSuccess(),
                r.getMessage()
        );
    }

    private br.dev.xb.isperp.dto.RadiusManualActionRequest fromApi(RadiusManualActionRequest req) {
        if (req == null) return null;
        return br.dev.xb.isperp.dto.RadiusManualActionRequest.builder()
                .contractId(req.getContractId())
                .action(req.getAction())
                .reason(req.getReason())
                .build();
    }

    private RadiusLifecycleLogResponse toApi(br.dev.xb.isperp.dto.RadiusLifecycleLogResponse l) {
        if (l == null) return null;
        RadiusLifecycleLogResponse resp = new RadiusLifecycleLogResponse();
        resp.setId(l.getId());
        resp.setContractId(l.getContractId());
        resp.setCustomerId(l.getCustomerId());
        resp.setCustomerName(l.getCustomerName());
        resp.setUsername(l.getUsername());
        if (l.getActionType() != null) {
            resp.setActionType(br.dev.xb.isperp.api.dto.RadiusLifecycleActionType.fromValue(l.getActionType().name()));
        }
        resp.setReason(l.getReason());
        resp.setNasIp(l.getNasIp());
        resp.setSuccess(l.isSuccess());
        resp.setDetails(l.getDetails());
        resp.setCreatedAt(l.getCreatedAt());
        return resp;
    }

    private RadiusLifecycleLogsPageResponse toPageResponse(Page<br.dev.xb.isperp.dto.RadiusLifecycleLogResponse> p) {
        if (p == null) return null;
        List<RadiusLifecycleLogResponse> content = p.getContent().stream()
                .map(this::toApi)
                .toList();
        return new RadiusLifecycleLogsPageResponse(
                content,
                p.getTotalElements(),
                p.getTotalPages(),
                p.getSize(),
                p.getNumber()
        );
    }
}
