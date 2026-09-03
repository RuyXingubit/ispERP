package br.dev.xb.isperp.dto.backup;

import br.dev.xb.isperp.backup.BackupStatus;
import br.dev.xb.isperp.backup.BackupTriggerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackupExecutionLogDto {
    private UUID id;
    private UUID policyId;
    @Nullable
    private UUID destinationId;
    @Nullable
    private String destinationName;
    private BackupTriggerType triggerType;
    private BackupStatus status;
    private String fileName;
    @Nullable
    private Long originalSizeBytes;
    @Nullable
    private Long compressedSizeBytes;
    @Nullable
    private BigDecimal compressionRatio;
    @Nullable
    private String sha256Hash;
    @Nullable
    private Integer durationSeconds;
    @Nullable
    private String errorMessage;
    private boolean isDryRunVerified;
    @Nullable
    private OffsetDateTime dryRunVerifiedAt;
    private OffsetDateTime startedAt;
    @Nullable
    private OffsetDateTime completedAt;
}
