package br.dev.xb.isperp.controller.backup;

import br.dev.xb.isperp.api.contract.BackupDisasterRecoveryApi;
import br.dev.xb.isperp.api.dto.*;
import br.dev.xb.isperp.backup.BackupTriggerType;
import br.dev.xb.isperp.entity.backup.BackupExecutionLog;
import br.dev.xb.isperp.mapper.DisasterRecoveryMapper;
import br.dev.xb.isperp.service.backup.BackupStreamingPipelineService;
import br.dev.xb.isperp.service.backup.DisasterRecoveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"", "/api"})
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BackupDisasterRecoveryController implements BackupDisasterRecoveryApi {

    private final DisasterRecoveryService disasterRecoveryService;
    private final BackupStreamingPipelineService pipelineService;
    private final DisasterRecoveryMapper disasterRecoveryMapper;

    @Override
    public ResponseEntity<BackupOverviewDto> getBackupOverview() {
        return ResponseEntity.ok(disasterRecoveryMapper.toOpenApiOverview(disasterRecoveryService.getOverview()));
    }

    @Override
    public ResponseEntity<BackupPolicyResponse> configureBackupPolicy(BackupPolicyRequest request) {
        var domainRequest = disasterRecoveryMapper.toDomainPolicyRequest(request);
        var domainResponse = disasterRecoveryService.configurePolicy(domainRequest);
        return ResponseEntity.ok(disasterRecoveryMapper.toOpenApiPolicyResponse(domainResponse));
    }

    @Override
    public ResponseEntity<List<BackupDestinationResponse>> listBackupDestinations() {
        var domainList = disasterRecoveryService.listDestinations();
        return ResponseEntity.ok(disasterRecoveryMapper.toOpenApiDestinationResponseList(domainList));
    }

    @Override
    public ResponseEntity<BackupDestinationResponse> createBackupDestination(BackupDestinationRequest request) {
        var domainRequest = disasterRecoveryMapper.toDomainDestinationRequest(request);
        var domainResponse = disasterRecoveryService.createDestination(domainRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(disasterRecoveryMapper.toOpenApiDestinationResponse(domainResponse));
    }

    @Override
    public ResponseEntity<StorageTestResult> testBackupDestination(UUID id) {
        var domainResult = disasterRecoveryService.testDestination(id);
        return ResponseEntity.ok(disasterRecoveryMapper.toOpenApiStorageTestResult(domainResult));
    }

    @Override
    public ResponseEntity<Void> deleteBackupDestination(UUID id) {
        disasterRecoveryService.deleteDestination(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<BackupExecutionLogDto> executeManualBackup(ExecuteBackupRequest request) {
        String customKey = request != null ? request.getMasterKey() : null;
        BackupExecutionLog log = pipelineService.executeBackup(BackupTriggerType.MANUAL, customKey);
        return ResponseEntity.ok(disasterRecoveryMapper.toOpenApiExecutionLogDto(log));
    }

    @Override
    public ResponseEntity<List<BackupExecutionLogDto>> listBackupHistory() {
        var domainLogs = disasterRecoveryService.listExecutionLogs();
        return ResponseEntity.ok(disasterRecoveryMapper.toOpenApiExecutionLogDtoList(domainLogs));
    }

    @Override
    public ResponseEntity<Resource> downloadEmergencyKit() {
        String content = disasterRecoveryService.generateEmergencyKitContent();
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        ByteArrayResource resource = new ByteArrayResource(bytes);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"KIT_RESGATE_EMERGENCIA_ISPERP.md\"")
                .contentType(new MediaType("text", "markdown", StandardCharsets.UTF_8))
                .body(resource);
    }
}
