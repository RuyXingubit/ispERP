package br.dev.xb.isperp.entity.backup;

import br.dev.xb.isperp.backup.CompressionAlgorithm;
import br.dev.xb.isperp.backup.SecurityMode;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import jakarta.persistence.*;
import lombok.*;
import org.jspecify.annotations.Nullable;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "backup_policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackupPolicy {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @Nullable
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "security_mode", nullable = false, length = 30)
    @Builder.Default
    private SecurityMode securityMode = SecurityMode.MANAGED_RESCUE;

    @Column(name = "master_key_hash", nullable = false, length = 128)
    private String masterKeyHash;

    @Column(name = "encrypted_master_key", columnDefinition = "TEXT")
    @Nullable
    private String encryptedMasterKey;

    @Column(name = "cron_expression", nullable = false, length = 50)
    @Builder.Default
    private String cronExpression = "0 0 3 * * *";

    @Column(name = "retention_days", nullable = false)
    @Builder.Default
    private Integer retentionDays = 30;

    @Enumerated(EnumType.STRING)
    @Column(name = "compression_algorithm", nullable = false, length = 20)
    @Builder.Default
    private CompressionAlgorithm compressionAlgorithm = CompressionAlgorithm.ZSTD;

    @Column(name = "auto_dry_run_enabled", nullable = false)
    @Builder.Default
    private Boolean autoDryRunEnabled = true;

    @Column(name = "rescue_kit_downloaded_at")
    @Nullable
    private OffsetDateTime rescueKitDownloadedAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Nullable
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @Nullable
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UuidCreatorUtils.generateUuidV7();
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = OffsetDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
