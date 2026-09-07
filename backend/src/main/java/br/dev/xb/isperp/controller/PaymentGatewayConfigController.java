package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.contract.PaymentGatewayConfigApi;
import br.dev.xb.isperp.api.dto.PaymentGatewayConfigResponse;
import br.dev.xb.isperp.api.dto.PaymentGatewayConfigSaveRequest;
import br.dev.xb.isperp.api.dto.PaymentGatewayType;
import br.dev.xb.isperp.entity.PaymentGatewayConfig;
import br.dev.xb.isperp.repository.PaymentGatewayConfigRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"/payment-gateways", "/api/payment-gateways"})
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@SuppressWarnings("null")
public class PaymentGatewayConfigController implements PaymentGatewayConfigApi {

    private final PaymentGatewayConfigRepository configRepository;

    @Override
    @GetMapping
    public ResponseEntity<List<PaymentGatewayConfigResponse>> getAllPaymentGatewayConfigs() {
        return ResponseEntity.ok(configRepository.findAll().stream()
                .map(this::toApiResponse)
                .collect(Collectors.toList()));
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<PaymentGatewayConfigResponse> getPaymentGatewayConfigById(@PathVariable UUID id) {
        Optional<PaymentGatewayConfig> config = configRepository.findById(id);
        return config.map(this::toApiResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    @PostMapping
    public ResponseEntity<PaymentGatewayConfigResponse> savePaymentGatewayConfig(@Valid @RequestBody PaymentGatewayConfigSaveRequest request) {
        PaymentGatewayConfig entity = toEntity(request);
        PaymentGatewayConfig saved = configRepository.save(entity);
        return ResponseEntity.ok(toApiResponse(saved));
    }

    private PaymentGatewayConfig toEntity(PaymentGatewayConfigSaveRequest req) {
        return PaymentGatewayConfig.builder()
                .id(req.getId())
                .companyId(req.getCompanyId())
                .gatewayType(req.getGatewayType() != null 
                        ? br.dev.xb.isperp.gateway.PaymentGatewayType.valueOf(req.getGatewayType().name()) 
                        : br.dev.xb.isperp.gateway.PaymentGatewayType.XINGUBIT_PAY)
                .name(req.getName())
                .apiKey(req.getApiKey())
                .secretKey(req.getSecretKey())
                .webhookSecret(req.getWebhookSecret())
                .pixKey(req.getPixKey())
                .sandbox(req.getSandbox() != null ? req.getSandbox() : false)
                .active(req.getActive() != null ? req.getActive() : true)
                .build();
    }

    private PaymentGatewayConfigResponse toApiResponse(PaymentGatewayConfig entity) {
        if (entity == null) return null;
        PaymentGatewayConfigResponse res = new PaymentGatewayConfigResponse();
        res.setId(entity.getId());
        res.setCompanyId(entity.getCompanyId());
        if (entity.getGatewayType() != null) {
            res.setGatewayType(PaymentGatewayType.fromValue(entity.getGatewayType().name()));
        }
        res.setName(entity.getName());
        res.setApiKey(entity.getApiKey());
        res.setSecretKey(entity.getSecretKey());
        res.setWebhookSecret(entity.getWebhookSecret());
        res.setPixKey(entity.getPixKey());
        res.setSandbox(entity.getSandbox());
        res.setActive(entity.getActive());
        if (entity.getCreatedAt() != null) {
            res.setCreatedAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC));
        }
        if (entity.getUpdatedAt() != null) {
            res.setUpdatedAt(entity.getUpdatedAt().atOffset(ZoneOffset.UTC));
        }
        return res;
    }
}
