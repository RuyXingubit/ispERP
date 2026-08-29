package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.entity.PaymentGatewayConfig;
import br.dev.xb.isperp.repository.PaymentGatewayConfigRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/payment-gateways")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@SuppressWarnings("null")
public class PaymentGatewayConfigController {

    private final PaymentGatewayConfigRepository configRepository;

    @GetMapping
    public ResponseEntity<List<PaymentGatewayConfig>> getAllConfigs() {
        return ResponseEntity.ok(configRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentGatewayConfig> getConfigById(@PathVariable UUID id) {
        Optional<PaymentGatewayConfig> config = configRepository.findById(id);
        return config.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<PaymentGatewayConfig> saveConfig(@Valid @RequestBody PaymentGatewayConfig config) {
        PaymentGatewayConfig saved = configRepository.save(config);
        return ResponseEntity.ok(saved);
    }
}
