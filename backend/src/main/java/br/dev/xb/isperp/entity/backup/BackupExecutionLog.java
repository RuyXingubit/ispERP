package br.dev.xb.isperp.entity.backup;

import br.dev.xb.isperp.backup.BackupStatus;
import br.dev.xb.isperp.backup.BackupTriggerType;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import jakarta.persistence.*;
import lombok.*;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "backup_execution_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackupExecutionLog {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @Nullable
    private UUID id;

    @Column(name = "policy_id", nullable = false)
    private UUID policyId;

    @Column(name = "destination_id")
    @Nullable
    private UUID destinationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 30)
    private BackupTriggerType triggerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private BackupStatus status;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "original_size_bytes")
    @Nullable
    private Long originalSizeBytes;

    @Column(name = "compressed_size_bytes")
    @Nullable
    private Long compressedSizeBytes;

    @Column(name = "compression_ratio", precision = 5, scale = 2)
    @Nullable
    private BigDecimal compressionRatio;

    @Column(name = "sha256_hash", length = 64)
    @Nullable
    private String sha256Hash;

    @Column(name = "duration_seconds")
    @Nullable
    private Integer durationSeconds;

    @Column(name = "error_message", columnDefinition = "TEXT")
    @Nullable
    private String errorMessage;

    @Column(name = "is_dry_run_verified", nullable = false)
    @Builder.Default
    private Boolean isDryRunVerified = false;

    @Column(name = "dry_run_verified_at")
    @Nullable
    private OffsetDateTime dryRunVerifiedAt;

    @Column(name = "started_at", nullable = false)
    @Builder.Default
    private OffsetDateTime startedAt = OffsetDateTime.now();

    @Column(name = "completed_at")
    @Nullable
    private OffsetDateTime completedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UuidCreatorUtils.generateUuidV7();
        }
        if (startedAt == null) {
            startedAt = OffsetDateTime.now();
        }
    }
}
