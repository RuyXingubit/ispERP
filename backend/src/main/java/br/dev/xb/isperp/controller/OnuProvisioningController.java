package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.entity.OnuProvisioning;
import br.dev.xb.isperp.network.dto.OnuStatusResponse;
import br.dev.xb.isperp.service.NetworkProvisioningService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/onus")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@SuppressWarnings("null")
public class OnuProvisioningController {

    private final NetworkProvisioningService provisioningService;

    @GetMapping
    public ResponseEntity<List<OnuProvisioning>> getAllOnus() {
        return ResponseEntity.ok(provisioningService.getAllProvisionings());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OnuProvisioning> getOnuById(@PathVariable @NonNull UUID id) {
        Optional<OnuProvisioning> onu = provisioningService.getProvisioningById(id);
        return onu.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/contract/{contractId}")
    public ResponseEntity<OnuProvisioning> getOnuByContractId(@PathVariable @NonNull UUID contractId) {
        Optional<OnuProvisioning> onu = provisioningService.getProvisioningByContractId(contractId);
        return onu.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/contract/{contractId}/block")
    public ResponseEntity<?> blockOnu(
            @PathVariable @NonNull UUID contractId,
            @RequestParam(defaultValue = "Inadimplência") String reason) {
        try {
            OnuProvisioning blocked = provisioningService.blockInternetAccess(contractId, reason);
            return ResponseEntity.ok(blocked);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/contract/{contractId}/unblock")
    public ResponseEntity<?> unblockOnu(@PathVariable @NonNull UUID contractId) {
        try {
            OnuProvisioning unblocked = provisioningService.unblockInternetAccess(contractId);
            return ResponseEntity.ok(unblocked);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}/diagnose")
    public ResponseEntity<OnuStatusResponse> diagnoseOnu(@PathVariable @NonNull UUID id) {
        try {
            OnuStatusResponse status = provisioningService.diagnoseOnuSignal(id);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
