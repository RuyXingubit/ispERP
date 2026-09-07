package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.contract.RadiusSessionsApi;
import br.dev.xb.isperp.api.dto.RadiusDisconnectRequest;
import br.dev.xb.isperp.api.dto.RadiusDisconnectResponse;
import br.dev.xb.isperp.api.dto.RadiusSessionResponse;
import br.dev.xb.isperp.service.RadiusSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"", "/api"})
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "RADIUS - Sessões & Accounting", description = "Monitoramento de Sessões Online e Desconexão PoD")
public class RadiusSessionController implements RadiusSessionsApi {

    private final RadiusSessionService radiusSessionService;

    @Override
    public ResponseEntity<List<RadiusSessionResponse>> getActiveSessions() {
        List<RadiusSessionResponse> list = radiusSessionService.getActiveSessions().stream()
                .map(this::toApi)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/radius/sessions/active/paged")
    @Operation(summary = "Lista sessões ativas paginadas")
    public ResponseEntity<Page<br.dev.xb.isperp.dto.RadiusSessionResponse>> getActiveSessionsPaged(Pageable pageable) {
        return ResponseEntity.ok(radiusSessionService.getActiveSessionsPaged(pageable));
    }

    @Override
    public ResponseEntity<List<RadiusSessionResponse>> getSessionHistory(String username) {
        List<RadiusSessionResponse> list = radiusSessionService.getSessionHistoryByUsername(username).stream()
                .map(this::toApi)
                .toList();
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<RadiusDisconnectResponse> disconnectUser(RadiusDisconnectRequest request) {
        br.dev.xb.isperp.dto.RadiusDisconnectResponse resp = radiusSessionService.disconnectUser(fromApi(request));
        return ResponseEntity.ok(toApi(resp));
    }

    private RadiusSessionResponse toApi(br.dev.xb.isperp.dto.RadiusSessionResponse s) {
        if (s == null) return null;
        RadiusSessionResponse resp = new RadiusSessionResponse();
        resp.setRadacctId(s.getRadacctId());
        resp.setAcctSessionId(s.getAcctSessionId());
        resp.setUsername(s.getUsername());
        resp.setNasIpAddress(s.getNasIpAddress());
        resp.setNasShortname(s.getNasShortname());
        resp.setAcctStartTime(s.getAcctStartTime());
        resp.setAcctUpdateTime(s.getAcctUpdateTime());
        resp.setAcctStopTime(s.getAcctStopTime());
        resp.setAcctSessionTime(s.getAcctSessionTime());
        resp.setAcctInputOctets(s.getAcctInputOctets());
        resp.setAcctOutputOctets(s.getAcctOutputOctets());
        resp.setCallingStationId(s.getCallingStationId());
        resp.setFramedIpAddress(s.getFramedIpAddress());
        resp.setFramedIpv6Prefix(s.getFramedIpv6Prefix());
        resp.setDelegatedIpv6Prefix(s.getDelegatedIpv6Prefix());
        resp.setIsOnline(s.isOnline());
        resp.setCustomerName(s.getCustomerName());
        resp.setCustomerCpfCnpj(s.getCustomerCpfCnpj());
        return resp;
    }

    private RadiusDisconnectResponse toApi(br.dev.xb.isperp.dto.RadiusDisconnectResponse r) {
        if (r == null) return null;
        return new RadiusDisconnectResponse(r.getUsername(), r.isSuccess(), r.getMessage());
    }

    private br.dev.xb.isperp.dto.RadiusDisconnectRequest fromApi(RadiusDisconnectRequest req) {
        if (req == null) return null;
        return br.dev.xb.isperp.dto.RadiusDisconnectRequest.builder()
                .username(req.getUsername())
                .nasIpAddress(req.getNasIpAddress())
                .framedIpAddress(req.getFramedIpAddress())
                .build();
    }
}
