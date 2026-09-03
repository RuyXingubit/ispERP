package br.dev.xb.isperp.dto.backup;

import br.dev.xb.isperp.backup.CompressionAlgorithm;
import br.dev.xb.isperp.backup.SecurityMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackupPolicyResponse {
    private UUID id;
    private SecurityMode securityMode;
    private String masterKeyHash;
    @Nullable
    private String generatedPlainMasterKey; // Apenas retornado na criação inicial ou atualização para o dono guardar
    private String cronExpression;
    private int retentionDays;
    private CompressionAlgorithm compressionAlgorithm;
    private boolean autoDryRunEnabled;
    private boolean isActive;
    @Nullable
    private OffsetDateTime rescueKitDownloadedAt;
    private OffsetDateTime createdAt;
}
