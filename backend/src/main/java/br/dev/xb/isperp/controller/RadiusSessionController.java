package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.dto.RadiusDisconnectRequest;
import br.dev.xb.isperp.dto.RadiusDisconnectResponse;
import br.dev.xb.isperp.dto.RadiusSessionResponse;
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
@RequestMapping({"/radius", "/api/radius"})
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "RADIUS - Sessões & Accounting", description = "Monitoramento de Sessões Online e Desconexão PoD")
public class RadiusSessionController {

    private final RadiusSessionService radiusSessionService;

    @GetMapping("/sessions/active")
    @Operation(summary = "Lista todas as sessões PPPoE/IPoE ativas (Online)")
    public ResponseEntity<List<RadiusSessionResponse>> getActiveSessions() {
        return ResponseEntity.ok(radiusSessionService.getActiveSessions());
    }

    @GetMapping("/sessions/active/paged")
    @Operation(summary = "Lista sessões ativas paginadas")
    public ResponseEntity<Page<RadiusSessionResponse>> getActiveSessionsPaged(Pageable pageable) {
        return ResponseEntity.ok(radiusSessionService.getActiveSessionsPaged(pageable));
    }

    @GetMapping("/sessions/history/{username}")
    @Operation(summary = "Histórico de conexões de um assinante")
    public ResponseEntity<List<RadiusSessionResponse>> getSessionHistory(@PathVariable String username) {
        return ResponseEntity.ok(radiusSessionService.getSessionHistoryByUsername(username));
    }

    @PostMapping("/disconnect")
    @Operation(summary = "Envia comando PoD (Packet of Disconnect) para derrubar conexão no BNG")
    public ResponseEntity<RadiusDisconnectResponse> disconnectUser(@Valid @RequestBody RadiusDisconnectRequest request) {
        return ResponseEntity.ok(radiusSessionService.disconnectUser(request));
    }
}
