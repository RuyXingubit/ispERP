package br.dev.xb.isperp.controller.backup;

import br.dev.xb.isperp.backup.BackupTriggerType;
import br.dev.xb.isperp.dto.backup.*;
import br.dev.xb.isperp.entity.backup.BackupExecutionLog;
import br.dev.xb.isperp.service.backup.BackupStreamingPipelineService;
import br.dev.xb.isperp.service.backup.DisasterRecoveryService;
import br.dev.xb.isperp.service.backup.StorageTestResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/financial/backup")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BackupDisasterRecoveryController {

    private final DisasterRecoveryService disasterRecoveryService;
    private final BackupStreamingPipelineService pipelineService;

    @GetMapping("/overview")
    public ResponseEntity<BackupOverviewDto> getOverview() {
        return ResponseEntity.ok(disasterRecoveryService.getOverview());
    }

    @PostMapping("/policies")
    public ResponseEntity<BackupPolicyResponse> configurePolicy(@Valid @RequestBody BackupPolicyRequest request) {
        return ResponseEntity.ok(disasterRecoveryService.configurePolicy(request));
    }

    @GetMapping("/destinations")
    public ResponseEntity<List<BackupDestinationResponse>> listDestinations() {
        return ResponseEntity.ok(disasterRecoveryService.listDestinations());
    }

    @PostMapping("/destinations")
    public ResponseEntity<BackupDestinationResponse> createDestination(@Valid @RequestBody BackupDestinationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(disasterRecoveryService.createDestination(request));
    }

    @PostMapping("/destinations/{id}/test")
    public ResponseEntity<StorageTestResult> testDestination(@PathVariable UUID id) {
        return ResponseEntity.ok(disasterRecoveryService.testDestination(id));
    }

    @DeleteMapping("/destinations/{id}")
    public ResponseEntity<Void> deleteDestination(@PathVariable UUID id) {
        disasterRecoveryService.deleteDestination(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/execute")
    public ResponseEntity<BackupExecutionLog> executeManualBackup(@RequestBody(required = false) Map<String, String> payload) {
        String customKey = payload != null ? payload.get("masterKey") : null;
        return ResponseEntity.ok(pipelineService.executeBackup(BackupTriggerType.MANUAL, customKey));
    }

    @GetMapping("/history")
    public ResponseEntity<List<BackupExecutionLogDto>> listHistory() {
        return ResponseEntity.ok(disasterRecoveryService.listExecutionLogs());
    }

    @GetMapping("/emergency-kit")
    public ResponseEntity<byte[]> downloadEmergencyKit() {
        String content = disasterRecoveryService.generateEmergencyKitContent();
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"KIT_RESGATE_EMERGENCIA_ISPERP.md\"")
                .contentType(MediaType.TEXT_MARKDOWN)
                .body(bytes);
    }
}
