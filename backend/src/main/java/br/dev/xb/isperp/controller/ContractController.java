package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.entity.Contract;
import br.dev.xb.isperp.service.ContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/contracts")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@SuppressWarnings("null")
public class ContractController {

    private final ContractService contractService;

    @GetMapping
    public ResponseEntity<List<Contract>> getAllContracts() {
        return ResponseEntity.ok(contractService.getAllContracts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Contract> getContractById(@PathVariable UUID id) {
        Optional<Contract> contract = contractService.getContractById(id);
        return contract.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Contract>> getContractsByCustomerId(@PathVariable UUID customerId) {
        return ResponseEntity.ok(contractService.getContractsByCustomerId(customerId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Contract>> getContractsByStatus(@PathVariable Contract.ContractStatus status) {
        return ResponseEntity.ok(contractService.getContractsByStatus(status));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateContractStatus(
            @PathVariable UUID id,
            @RequestParam Contract.ContractStatus status) {
        try {
            Contract updated = contractService.updateStatus(id, status);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
