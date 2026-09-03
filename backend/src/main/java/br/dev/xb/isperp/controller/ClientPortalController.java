package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.dto.*;
import br.dev.xb.isperp.entity.Contract;
import br.dev.xb.isperp.entity.Customer;
import br.dev.xb.isperp.entity.TrustUnblock;
import br.dev.xb.isperp.service.ClientPortalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/portal/client")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@SuppressWarnings("null")
public class ClientPortalController {

    private final ClientPortalService clientPortalService;

    /**
     * Autenticação do cliente por CPF/CNPJ com validação de PIN de 4 dígitos.
     */
    @PostMapping("/auth")
    public ResponseEntity<ClientAuthResponse> authenticate(@Valid @RequestBody ClientAuthRequest request) {
        return ResponseEntity.ok(clientPortalService.authenticateClient(request));
    }

    /**
     * Define ou atualiza o PIN de 4 dígitos do cliente.
     */
    @PostMapping("/pin")
    public ResponseEntity<Map<String, String>> setPin(@Valid @RequestBody SetClientPinRequest request) {
        clientPortalService.setPin(request);
        return ResponseEntity.ok(Map.of("message", "PIN de 4 dígitos configurado com sucesso."));
    }

    /**
     * Retorna o dashboard completo do assinante.
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ClientPortalDashboardDTO> getDashboard(
            @RequestParam(required = false) @Nullable UUID customerId,
            @RequestHeader(value = "X-Customer-Id", required = false) @Nullable UUID headerCustomerId) {

        UUID targetId = resolveCustomerId(customerId, headerCustomerId);
        ClientPortalDashboardDTO dashboard = clientPortalService.getClientDashboard(targetId);
        return ResponseEntity.ok(dashboard);
    }

    /**
     * Atualiza dados de contato do cliente.
     */
    @PutMapping("/profile")
    public ResponseEntity<Customer> updateProfile(
            @RequestParam(required = false) @Nullable UUID customerId,
            @RequestHeader(value = "X-Customer-Id", required = false) @Nullable UUID headerCustomerId,
            @Valid @RequestBody UpdateClientProfileRequest request) {

        UUID targetId = resolveCustomerId(customerId, headerCustomerId);
        Customer updated = clientPortalService.updateProfile(targetId, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Altera a senha do assinante.
     */
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @RequestParam(required = false) @Nullable UUID customerId,
            @RequestHeader(value = "X-Customer-Id", required = false) @Nullable UUID headerCustomerId,
            @Valid @RequestBody ChangePasswordRequest request) {

        UUID targetId = resolveCustomerId(customerId, headerCustomerId);
        clientPortalService.changePassword(targetId, request);
        return ResponseEntity.ok(Map.of("message", "Senha alterada com sucesso"));
    }

    /**
     * Executa solicitação de Upgrade de Plano pelo cliente.
     */
    @PostMapping("/upgrade-plan")
    public ResponseEntity<Contract> upgradePlan(
            @RequestParam(required = false) @Nullable UUID customerId,
            @RequestHeader(value = "X-Customer-Id", required = false) @Nullable UUID headerCustomerId,
            @RequestBody Map<String, UUID> payload) {

        UUID targetId = resolveCustomerId(customerId, headerCustomerId);
        UUID contractId = payload.get("contractId");
        UUID newPlanId = payload.get("newPlanId");

        Contract updated = clientPortalService.requestPlanUpgrade(targetId, contractId, newPlanId);
        return ResponseEntity.ok(updated);
    }

    /**
     * Solicita o Desbloqueio em Confiança (48h).
     */
    @PostMapping("/trust-unblock")
    public ResponseEntity<TrustUnblock> requestTrustUnblock(
            @RequestParam(required = false) @Nullable UUID customerId,
            @RequestHeader(value = "X-Customer-Id", required = false) @Nullable UUID headerCustomerId,
            @RequestBody Map<String, UUID> payload) {

        UUID targetId = resolveCustomerId(customerId, headerCustomerId);
        UUID contractId = payload.get("contractId");

        TrustUnblock trustUnblock = clientPortalService.requestTrustUnblock(targetId, contractId);
        return ResponseEntity.ok(trustUnblock);
    }

    private UUID resolveCustomerId(@Nullable UUID queryId, @Nullable UUID headerId) {
        if (queryId != null) return queryId;
        if (headerId != null) return headerId;

        // Sem fallback: acesso não identificado é estritamente rejeitado com 401 Unauthorized
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Acesso não autorizado. Identifique-se com seu CPF ou CNPJ.");
    }
}
