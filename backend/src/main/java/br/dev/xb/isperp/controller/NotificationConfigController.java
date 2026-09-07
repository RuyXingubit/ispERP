package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.contract.NotificationConfigsApi;
import br.dev.xb.isperp.api.dto.NotificationConfigResponse;
import br.dev.xb.isperp.api.dto.NotificationConfigSaveRequest;
import br.dev.xb.isperp.api.dto.WhatsAppProviderType;
import br.dev.xb.isperp.entity.NotificationConfig;
import br.dev.xb.isperp.repository.NotificationConfigRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"/notifications/configs", "/api/notifications/configs"})
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@SuppressWarnings("null")
public class NotificationConfigController implements NotificationConfigsApi {

    private final NotificationConfigRepository configRepository;

    @Override
    @GetMapping
    public ResponseEntity<List<NotificationConfigResponse>> listNotificationConfigs() {
        return ResponseEntity.ok(configRepository.findAll().stream()
                .map(this::toApiResponse)
                .collect(Collectors.toList()));
    }

    @Override
    @PostMapping
    public ResponseEntity<NotificationConfigResponse> createNotificationConfig(@Valid @RequestBody NotificationConfigSaveRequest request) {
        NotificationConfig entity = toEntity(request);
        NotificationConfig saved = configRepository.save(entity);
        return ResponseEntity.ok(toApiResponse(saved));
    }

    @Override
    @PutMapping("/{id}")
    public ResponseEntity<NotificationConfigResponse> updateNotificationConfig(
            @PathVariable UUID id,
            @Valid @RequestBody NotificationConfigSaveRequest request
    ) {
        NotificationConfig entity = toEntity(request);
        entity.setId(id);
        NotificationConfig saved = configRepository.save(entity);
        return ResponseEntity.ok(toApiResponse(saved));
    }

    private NotificationConfig toEntity(NotificationConfigSaveRequest req) {
        return NotificationConfig.builder()
                .companyId(req.getCompanyId())
                .providerType(req.getProviderType() != null 
                        ? br.dev.xb.isperp.notification.whatsapp.WhatsAppProviderType.valueOf(req.getProviderType().name()) 
                        : br.dev.xb.isperp.notification.whatsapp.WhatsAppProviderType.TWILIO)
                .name(req.getName())
                .apiUrl(req.getApiUrl())
                .apiToken(req.getApiToken())
                .accountSid(req.getAccountSid())
                .authToken(req.getAuthToken())
                .fromPhoneNumber(req.getFromPhoneNumber())
                .active(req.getActive() != null ? req.getActive() : true)
                .build();
    }

    private NotificationConfigResponse toApiResponse(NotificationConfig entity) {
        if (entity == null) return null;
        NotificationConfigResponse res = new NotificationConfigResponse();
        res.setId(entity.getId());
        res.setCompanyId(entity.getCompanyId());
        if (entity.getProviderType() != null) {
            res.setProviderType(WhatsAppProviderType.fromValue(entity.getProviderType().name()));
        }
        res.setName(entity.getName());
        res.setApiUrl(entity.getApiUrl());
        res.setApiToken(entity.getApiToken());
        res.setAccountSid(entity.getAccountSid());
        res.setAuthToken(entity.getAuthToken());
        res.setFromPhoneNumber(entity.getFromPhoneNumber());
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
