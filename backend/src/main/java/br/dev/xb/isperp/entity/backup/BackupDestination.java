package br.dev.xb.isperp.entity.backup;

import br.dev.xb.isperp.backup.StorageType;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import jakarta.persistence.*;
import lombok.*;
import org.jspecify.annotations.Nullable;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "backup_destinations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackupDestination {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @Nullable
    private UUID id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_type", nullable = false, length = 30)
    @Builder.Default
    private StorageType storageType = StorageType.S3_COMPATIBLE;

    @Column(name = "endpoint_url")
    @Nullable
    private String endpointUrl;

    @Column(name = "bucket_name", length = 100)
    @Nullable
    private String bucketName;

    @Column(name = "region", length = 50)
    @Builder.Default
    private String region = "auto";

    @Column(name = "access_key")
    @Nullable
    private String accessKey;

    @Column(name = "secret_key_encrypted", columnDefinition = "TEXT")
    @Nullable
    private String secretKeyEncrypted;

    @Column(name = "path_prefix")
    @Builder.Default
    private String pathPrefix = "backups/isperp";

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;

    @Column(name = "last_tested_at")
    @Nullable
    private OffsetDateTime lastTestedAt;

    @Column(name = "last_test_status", length = 20)
    @Nullable
    private String lastTestStatus;

    @Column(name = "last_test_error", columnDefinition = "TEXT")
    @Nullable
    private String lastTestError;

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
