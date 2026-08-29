package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.dto.ChangePasswordRequest;
import br.dev.xb.isperp.dto.ClientPortalDashboardDTO;
import br.dev.xb.isperp.dto.UpdateClientProfileRequest;
import br.dev.xb.isperp.entity.Contract;
import br.dev.xb.isperp.entity.Customer;
import br.dev.xb.isperp.entity.TrustUnblock;
import br.dev.xb.isperp.repository.CustomerRepository;
import br.dev.xb.isperp.service.ClientPortalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/portal/client")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ClientPortalController {

    private final ClientPortalService clientPortalService;
    private final CustomerRepository customerRepository;

    /**
     * Retorna o dashboard completo do assinante.
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ClientPortalDashboardDTO> getDashboard(
            @RequestParam(required = false) UUID customerId,
            @RequestHeader(value = "X-Customer-Id", required = false) UUID headerCustomerId) {

        UUID targetId = resolveCustomerId(customerId, headerCustomerId);
        ClientPortalDashboardDTO dashboard = clientPortalService.getClientDashboard(targetId);
        return ResponseEntity.ok(dashboard);
    }

    /**
     * Atualiza dados de contato do cliente.
     */
    @PutMapping("/profile")
    public ResponseEntity<Customer> updateProfile(
            @RequestParam(required = false) UUID customerId,
            @RequestHeader(value = "X-Customer-Id", required = false) UUID headerCustomerId,
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
            @RequestParam(required = false) UUID customerId,
            @RequestHeader(value = "X-Customer-Id", required = false) UUID headerCustomerId,
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
            @RequestParam(required = false) UUID customerId,
            @RequestHeader(value = "X-Customer-Id", required = false) UUID headerCustomerId,
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
            @RequestParam(required = false) UUID customerId,
            @RequestHeader(value = "X-Customer-Id", required = false) UUID headerCustomerId,
            @RequestBody Map<String, UUID> payload) {

        UUID targetId = resolveCustomerId(customerId, headerCustomerId);
        UUID contractId = payload.get("contractId");

        TrustUnblock trustUnblock = clientPortalService.requestTrustUnblock(targetId, contractId);
        return ResponseEntity.ok(trustUnblock);
    }

    private UUID resolveCustomerId(UUID queryId, UUID headerId) {
        if (queryId != null) return queryId;
        if (headerId != null) return headerId;

        // Fallback para o primeiro cliente cadastrado para demonstração/testes
        return customerRepository.findAll().stream()
                .findFirst()
                .map(c -> c.getId())
                .orElseThrow(() -> new RuntimeException("Nenhum cliente cadastrado no sistema"));
    }
}
