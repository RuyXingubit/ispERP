package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.entity.NotificationConfig;
import br.dev.xb.isperp.repository.NotificationConfigRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/notifications/configs")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@SuppressWarnings("null")
public class NotificationConfigController {

    private final NotificationConfigRepository configRepository;

    @GetMapping
    public ResponseEntity<List<NotificationConfig>> listConfigs() {
        return ResponseEntity.ok(configRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<NotificationConfig> createConfig(@Valid @RequestBody NotificationConfig config) {
        return ResponseEntity.ok(configRepository.save(config));
    }

    @PutMapping("/{id}")
    public ResponseEntity<NotificationConfig> updateConfig(@PathVariable UUID id, @Valid @RequestBody NotificationConfig config) {
        config.setId(id);
        return ResponseEntity.ok(configRepository.save(config));
    }
}
