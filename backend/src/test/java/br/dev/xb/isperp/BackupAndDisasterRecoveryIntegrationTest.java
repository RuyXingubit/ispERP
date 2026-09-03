package br.dev.xb.isperp;

import br.dev.xb.isperp.backup.BackupStatus;
import br.dev.xb.isperp.backup.BackupTriggerType;
import br.dev.xb.isperp.backup.CompressionAlgorithm;
import br.dev.xb.isperp.backup.SecurityMode;
import br.dev.xb.isperp.backup.StorageType;
import br.dev.xb.isperp.dto.backup.BackupDestinationRequest;
import br.dev.xb.isperp.dto.backup.BackupDestinationResponse;
import br.dev.xb.isperp.dto.backup.BackupOverviewDto;
import br.dev.xb.isperp.dto.backup.BackupPolicyRequest;
import br.dev.xb.isperp.dto.backup.BackupPolicyResponse;
import br.dev.xb.isperp.entity.backup.BackupExecutionLog;
import br.dev.xb.isperp.entity.backup.BackupPolicy;
import br.dev.xb.isperp.repository.backup.BackupExecutionLogRepository;
import br.dev.xb.isperp.repository.backup.BackupPolicyRepository;
import br.dev.xb.isperp.service.backup.BackupCryptoService;
import br.dev.xb.isperp.service.backup.BackupStreamingPipelineService;
import br.dev.xb.isperp.service.backup.DisasterRecoveryService;
import br.dev.xb.isperp.service.backup.StorageTestResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("null")
class BackupAndDisasterRecoveryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private DisasterRecoveryService disasterRecoveryService;

    @Autowired
    private BackupStreamingPipelineService pipelineService;

    @Autowired
    private BackupPolicyRepository policyRepository;

    @Autowired
    private BackupExecutionLogRepository logRepository;

    @Autowired
    private BackupCryptoService cryptoService;

    @Test
    @DisplayName("Valida no PostgreSQL 17 real: Pipeline de Streaming, Criptografia AES-256, ZSTD, Teste de Conexão e Kit de Resgate")
    void shouldExecuteFullBackupAndDisasterRecoveryCycleOnPostgres17(@TempDir Path tempDir) throws Exception {
        // 1. Configurar Política Central de Backup
        BackupPolicyRequest policyRequest = BackupPolicyRequest.builder()
                .securityMode(SecurityMode.MANAGED_RESCUE)
                .cronExpression("0 0 3 * * *")
                .retentionDays(30)
                .compressionAlgorithm(CompressionAlgorithm.ZSTD)
                .autoDryRunEnabled(true)
                .build();

        BackupPolicyResponse policyResponse = disasterRecoveryService.configurePolicy(policyRequest);
        assertThat(policyResponse.getId()).isNotNull();
        assertThat(policyResponse.getGeneratedPlainMasterKey()).isNotBlank();
        assertThat(policyResponse.getMasterKeyHash()).hasSize(64);

        // 2. Configurar Destino de Armazenamento Local no diretório temporário
        BackupDestinationRequest destRequest = BackupDestinationRequest.builder()
                .name("Storage Local Temporário de Alta Confiabilidade")
                .storageType(StorageType.LOCAL_VOLUME)
                .pathPrefix(tempDir.toAbsolutePath().toString())
                .isPrimary(true)
                .build();

        BackupDestinationResponse destResponse = disasterRecoveryService.createDestination(destRequest);
        assertThat(destResponse.getId()).isNotNull();
        assertThat(destResponse.getName()).isEqualTo("Storage Local Temporário de Alta Confiabilidade");

        // 3. Testar Conexão em Tempo Real do Storage
        StorageTestResult testResult = disasterRecoveryService.testDestination(destResponse.getId());
        assertThat(testResult.isSuccess()).isTrue();
        assertThat(testResult.getMessage()).contains("sucesso");

        // 4. Executar Backup Manual Imediato via Pipeline Streaming com Compressão ZSTD e AES-256
        BackupExecutionLog logEntry = pipelineService.executeBackup(BackupTriggerType.MANUAL, null);

        assertThat(logEntry.getStatus()).isEqualTo(BackupStatus.SUCCESS);
        assertThat(logEntry.getFileName()).contains(".sql.zst.enc");
        assertThat(logEntry.getOriginalSizeBytes()).isGreaterThan(0);
        assertThat(logEntry.getCompressedSizeBytes()).isGreaterThan(0);
        assertThat(logEntry.getSha256Hash()).hasSize(64);
        assertThat(logEntry.getCompletedAt()).isNotNull();

        // 5. Verificar se o arquivo físico foi gravado no destino com criptografia
        File backupFile = tempDir.resolve(logEntry.getFileName()).toFile();
        assertThat(backupFile).exists();
        assertThat(backupFile.length()).isEqualTo(logEntry.getCompressedSizeBytes());

        // 6. Teste de Restauração e Verificação de Integridade (Dry-Run)
        BackupPolicy activePolicy = policyRepository.findById(policyResponse.getId()).orElseThrow();
        String plainKey = cryptoService.decryptSystemSecret(activePolicy.getEncryptedMasterKey());

        try (FileInputStream fis = new FileInputStream(backupFile)) {
            boolean isValid = pipelineService.verifyIntegrity(fis, plainKey, CompressionAlgorithm.ZSTD);
            assertThat(isValid).isTrue();
        }

        // Marcar auditoria de dry-run
        logEntry.setIsDryRunVerified(true);
        logEntry.setDryRunVerifiedAt(java.time.OffsetDateTime.now());
        logRepository.save(logEntry);

        // 7. Emitir Kit de Resgate de Emergência em Markdown
        String emergencyKit = disasterRecoveryService.generateEmergencyKitContent();
        assertThat(emergencyKit).contains("KIT DE RESGATE DE EMERGÊNCIA & DISASTER RECOVERY");
        assertThat(emergencyKit).contains("CHAVE MESTRA DE DESCRIPTOGRAFIA");
        assertThat(emergencyKit).contains(plainKey);
        assertThat(emergencyKit).contains("openssl enc -d -aes-256-cbc -pbkdf2");

        // 8. Consultar Visão Geral do Cockpit
        BackupOverviewDto overview = disasterRecoveryService.getOverview();
        assertThat(overview.isHasActivePolicy()).isTrue();
        assertThat(overview.getSecurityMode()).isEqualTo(SecurityMode.MANAGED_RESCUE);
        assertThat(overview.getLastBackupStatus()).isEqualTo("SUCCESS");
        assertThat(overview.getLastBackupSizeBytes()).isGreaterThan(0);
        assertThat(overview.isDryRunVerified()).isTrue();
        assertThat(overview.isRescueKitDownloaded()).isTrue();
        assertThat(overview.getActiveDestinationsCount()).isGreaterThanOrEqualTo(1);
    }
}
