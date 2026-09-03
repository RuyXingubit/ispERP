package br.dev.xb.isperp.service.backup;

import br.dev.xb.isperp.backup.BackupStatus;
import br.dev.xb.isperp.backup.SecurityMode;
import br.dev.xb.isperp.dto.backup.*;
import br.dev.xb.isperp.entity.backup.BackupDestination;
import br.dev.xb.isperp.entity.backup.BackupExecutionLog;
import br.dev.xb.isperp.entity.backup.BackupPolicy;
import br.dev.xb.isperp.repository.backup.BackupDestinationRepository;
import br.dev.xb.isperp.repository.backup.BackupExecutionLogRepository;
import br.dev.xb.isperp.repository.backup.BackupPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DisasterRecoveryService {

    private final BackupPolicyRepository policyRepository;
    private final BackupDestinationRepository destinationRepository;
    private final BackupExecutionLogRepository logRepository;
    private final BackupCryptoService cryptoService;
    private final StorageProviderService storageProviderService;

    /**
     * Retorna a visão geral do Cockpit de Backup & Disaster Recovery.
     */
    @Transactional(readOnly = true)
    public BackupOverviewDto getOverview() {
        var policyOpt = policyRepository.findFirstByIsActiveTrueOrderByCreatedAtDesc();
        var lastSuccessOpt = logRepository.findFirstByStatusOrderByCompletedAtDesc(BackupStatus.SUCCESS);
        var lastDryRunOpt = logRepository.findFirstByIsDryRunVerifiedTrueOrderByDryRunVerifiedAtDesc();
        int activeDestCount = destinationRepository.findByIsActiveTrue().size();
        long totalCount = logRepository.count();

        if (policyOpt.isEmpty()) {
            return BackupOverviewDto.builder()
                    .hasActivePolicy(false)
                    .securityMode(SecurityMode.MANAGED_RESCUE)
                    .cronExpression("0 0 3 * * *")
                    .retentionDays(30)
                    .activeDestinationsCount(activeDestCount)
                    .totalBackupsCount(totalCount)
                    .build();
        }

        BackupPolicy policy = policyOpt.get();
        return BackupOverviewDto.builder()
                .hasActivePolicy(true)
                .securityMode(policy.getSecurityMode())
                .cronExpression(policy.getCronExpression())
                .retentionDays(policy.getRetentionDays())
                .activeDestinationsCount(activeDestCount)
                .totalBackupsCount(totalCount)
                .lastBackupStatus(lastSuccessOpt.map(l -> l.getStatus().name()).orElse(null))
                .lastBackupAt(lastSuccessOpt.map(BackupExecutionLog::getCompletedAt).orElse(null))
                .lastBackupSizeBytes(lastSuccessOpt.map(BackupExecutionLog::getCompressedSizeBytes).orElse(null))
                .lastBackupCompressionRatio(lastSuccessOpt.map(BackupExecutionLog::getCompressionRatio).orElse(null))
                .lastBackupSha256(lastSuccessOpt.map(BackupExecutionLog::getSha256Hash).orElse(null))
                .lastBackupFileName(lastSuccessOpt.map(BackupExecutionLog::getFileName).orElse(null))
                .isDryRunVerified(lastDryRunOpt.isPresent())
                .lastDryRunVerifiedAt(lastDryRunOpt.map(BackupExecutionLog::getDryRunVerifiedAt).orElse(null))
                .rescueKitDownloaded(policy.getRescueKitDownloadedAt() != null)
                .build();
    }

    /**
     * Cria ou atualiza a Política Central de Backup do provedor.
     */
    @Transactional
    public BackupPolicyResponse configurePolicy(BackupPolicyRequest request) {
        String masterKey = request.getCustomMasterKey();
        String generatedKey = null;

        if (masterKey == null || masterKey.isBlank()) {
            generatedKey = cryptoService.generateMasterKey();
            masterKey = generatedKey;
        }

        String masterKeyHash = cryptoService.calculateSha256(masterKey);
        String encryptedKey = request.getSecurityMode() == SecurityMode.MANAGED_RESCUE
                ? cryptoService.encryptSystemSecret(masterKey)
                : null;

        BackupPolicy policy = policyRepository.findFirstByIsActiveTrueOrderByCreatedAtDesc()
                .orElse(BackupPolicy.builder().build());

        policy.setSecurityMode(request.getSecurityMode());
        policy.setMasterKeyHash(masterKeyHash);
        policy.setEncryptedMasterKey(encryptedKey);
        policy.setCronExpression(request.getCronExpression());
        policy.setRetentionDays(request.getRetentionDays());
        policy.setCompressionAlgorithm(request.getCompressionAlgorithm());
        policy.setAutoDryRunEnabled(request.isAutoDryRunEnabled());
        policy.setIsActive(true);

        BackupPolicy saved = policyRepository.save(policy);

        log.info("Política de backup atualizada: Modo {}, Algoritmo {}, Retenção {} dias",
                saved.getSecurityMode(), saved.getCompressionAlgorithm(), saved.getRetentionDays());

        return BackupPolicyResponse.builder()
                .id(saved.getId())
                .securityMode(saved.getSecurityMode())
                .masterKeyHash(saved.getMasterKeyHash())
                .generatedPlainMasterKey(generatedKey)
                .cronExpression(saved.getCronExpression())
                .retentionDays(saved.getRetentionDays())
                .compressionAlgorithm(saved.getCompressionAlgorithm())
                .autoDryRunEnabled(saved.getAutoDryRunEnabled())
                .isActive(saved.getIsActive())
                .rescueKitDownloadedAt(saved.getRescueKitDownloadedAt())
                .createdAt(saved.getCreatedAt() != null ? saved.getCreatedAt() : OffsetDateTime.now())
                .build();
    }

    /**
     * Lista destinos remotos configurados.
     */
    @Transactional(readOnly = true)
    public List<BackupDestinationResponse> listDestinations() {
        return destinationRepository.findAll().stream()
                .map(this::toDestinationResponse)
                .toList();
    }

    /**
     * Cadastra um novo destino de armazenamento.
     */
    @Transactional
    public BackupDestinationResponse createDestination(BackupDestinationRequest request) {
        String encryptedSecret = request.getSecretKey() != null
                ? cryptoService.encryptSystemSecret(request.getSecretKey())
                : null;

        if (request.isPrimary()) {
            destinationRepository.findByIsActiveTrue().forEach(d -> {
                if (d.getIsPrimary()) {
                    d.setIsPrimary(false);
                    destinationRepository.save(d);
                }
            });
        }

        BackupDestination destination = BackupDestination.builder()
                .name(request.getName())
                .storageType(request.getStorageType())
                .endpointUrl(request.getEndpointUrl())
                .bucketName(request.getBucketName())
                .region(request.getRegion())
                .accessKey(request.getAccessKey())
                .secretKeyEncrypted(encryptedSecret)
                .pathPrefix(request.getPathPrefix())
                .isActive(true)
                .isPrimary(request.isPrimary())
                .build();

        BackupDestination saved = destinationRepository.save(destination);
        log.info("Destino de backup adicionado: {} ({})", saved.getName(), saved.getStorageType());
        return toDestinationResponse(saved);
    }

    /**
     * Testa em tempo real a conectividade do destino.
     */
    @Transactional
    public StorageTestResult testDestination(UUID destinationId) {
        BackupDestination destination = destinationRepository.findById(destinationId)
                .orElseThrow(() -> new NoSuchElementException("Destino não encontrado com ID: " + destinationId));

        StorageTestResult result = storageProviderService.testConnection(destination);
        destination.setLastTestedAt(OffsetDateTime.now());
        destination.setLastTestStatus(result.isSuccess() ? "SUCCESS" : "FAILED");
        destination.setLastTestError(result.getDetailedError());
        destinationRepository.save(destination);

        return result;
    }

    /**
     * Remove um destino de armazenamento.
     */
    @Transactional
    public void deleteDestination(UUID destinationId) {
        destinationRepository.deleteById(destinationId);
    }

    /**
     * Retorna histórico de backups e simulações.
     */
    @Transactional(readOnly = true)
    public List<BackupExecutionLogDto> listExecutionLogs() {
        Map<UUID, String> destinationNames = destinationRepository.findAll().stream()
                .filter(d -> d.getId() != null)
                .collect(Collectors.toMap(BackupDestination::getId, BackupDestination::getName));

        return logRepository.findAllByOrderByStartedAtDesc().stream()
                .map(log -> BackupExecutionLogDto.builder()
                        .id(log.getId())
                        .policyId(log.getPolicyId())
                        .destinationId(log.getDestinationId())
                        .destinationName(log.getDestinationId() != null ? destinationNames.get(log.getDestinationId()) : null)
                        .triggerType(log.getTriggerType())
                        .status(log.getStatus())
                        .fileName(log.getFileName())
                        .originalSizeBytes(log.getOriginalSizeBytes())
                        .compressedSizeBytes(log.getCompressedSizeBytes())
                        .compressionRatio(log.getCompressionRatio())
                        .sha256Hash(log.getSha256Hash())
                        .durationSeconds(log.getDurationSeconds())
                        .errorMessage(log.getErrorMessage())
                        .isDryRunVerified(log.getIsDryRunVerified())
                        .dryRunVerifiedAt(log.getDryRunVerifiedAt())
                        .startedAt(log.getStartedAt())
                        .completedAt(log.getCompletedAt())
                        .build())
                .toList();
    }

    /**
     * Gera o documento Markdown do Kit de Resgate de Emergência com Chave Mestra e comandos OpenSSL.
     */
    @Transactional
    public String generateEmergencyKitContent() {
        BackupPolicy policy = policyRepository.findFirstByIsActiveTrueOrderByCreatedAtDesc()
                .orElseThrow(() -> new IllegalStateException("Nenhuma política de backup configurada."));

        String plainKey = policy.getSecurityMode() == SecurityMode.MANAGED_RESCUE && policy.getEncryptedMasterKey() != null
                ? cryptoService.decryptSystemSecret(policy.getEncryptedMasterKey())
                : "[MODO ZERO-KNOWLEDGE: Sua chave mestra original definida no momento da configuração]";

        policy.setRescueKitDownloadedAt(OffsetDateTime.now());
        policyRepository.save(policy);

        return buildEmergencyKitMarkdown(policy, plainKey);
    }

    private String buildEmergencyKitMarkdown(BackupPolicy policy, String plainKey) {
        StringBuilder sb = new StringBuilder();
        sb.append("# 🚨 KIT DE RESGATE DE EMERGÊNCIA & DISASTER RECOVERY (ispERP)\n\n");
        sb.append("> **DOCUMENTO CONFIDENCIAL DE SEGURANÇA MÁXIMA**  \n");
        sb.append("> Imprima este documento ou salve-o em um pendrive criptografado guardado no cofre físico da empresa.\n\n");
        sb.append("---\n\n");
        sb.append("## 1. Dados da Sua Política de Proteção Patrimonial\n\n");
        sb.append("- **Modo de Segurança:** `").append(policy.getSecurityMode()).append("`\n");
        sb.append("- **Algoritmo de Compressão:** `").append(policy.getCompressionAlgorithm()).append("`\n");
        sb.append("- **Agendamento:** Diário às ").append(policy.getCronExpression()).append("\n");
        sb.append("- **Hash de Identificação da Chave (SHA-256):**\n");
        sb.append("  `").append(policy.getMasterKeyHash()).append("`\n\n");
        sb.append("### 🔑 CHAVE MESTRA DE DESCRIPTOGRAFIA (AES-256):\n");
        sb.append("```text\n");
        sb.append(plainKey).append("\n");
        sb.append("```\n\n");
        sb.append("---\n\n");
        sb.append("## 2. Instruções de Restauração Manual em Caso de Desastre Total\n\n");
        sb.append("Se o servidor central do provedor queimar ou sofrer pane irreversível, qualquer máquina Linux com OpenSSL e PostgreSQL pode restaurar o banco usando este comando:\n\n");
        sb.append("```bash\n");
        sb.append("# 1. Descriptografa e descomprime o backup direto para o PostgreSQL\n");
        sb.append("openssl enc -d -aes-256-cbc -pbkdf2 -in isperp_backup_ARQUIVO.sql.zst.enc -pass pass:\"").append(plainKey).append("\" \\\n");
        sb.append("  | zstd -d \\\n");
        sb.append("  | psql -h localhost -U isperp isperp_db\n");
        sb.append("```\n\n");
        sb.append("> **Suporte de Contingência ispERP:** Em caso de perda total da infraestrutura, a equipe do ispERP provê uma VPS de contingência em nuvem com 15 dias de cortesia para reestabelecer sua operação em menos de 1 hora.\n");
        return sb.toString();
    }

    private BackupDestinationResponse toDestinationResponse(BackupDestination d) {
        return BackupDestinationResponse.builder()
                .id(d.getId())
                .name(d.getName())
                .storageType(d.getStorageType())
                .endpointUrl(d.getEndpointUrl())
                .bucketName(d.getBucketName())
                .region(d.getRegion() != null ? d.getRegion() : "auto")
                .accessKey(d.getAccessKey())
                .pathPrefix(d.getPathPrefix() != null ? d.getPathPrefix() : "backups/isperp")
                .isActive(d.getIsActive())
                .isPrimary(d.getIsPrimary())
                .lastTestedAt(d.getLastTestedAt())
                .lastTestStatus(d.getLastTestStatus())
                .lastTestError(d.getLastTestError())
                .createdAt(d.getCreatedAt() != null ? d.getCreatedAt() : OffsetDateTime.now())
                .build();
    }
}
