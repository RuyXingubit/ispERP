package br.dev.xb.isperp.entity;

import br.dev.xb.isperp.storage.StorageProvider;
import br.dev.xb.isperp.storage.StorageType;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "storage_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SuppressWarnings("null")
public class StorageConfig {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @Nullable
    private UUID id;

    @Column(name = "company_id")
    @Nullable
    private UUID companyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_type", nullable = false, length = 50)
    @Builder.Default
    private StorageType storageType = StorageType.S3;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 50)
    @Builder.Default
    private StorageProvider provider = StorageProvider.SEAWEEDFS_LOCAL;

    @Column(name = "endpoint_url")
    @Nullable
    private String endpointUrl;

    @Column(name = "bucket_name", nullable = false, length = 100)
    @Builder.Default
    private String bucketName = "isperp-files";

    @Column(name = "region", nullable = false, length = 50)
    @Builder.Default
    private String region = "us-east-1";

    @Column(name = "access_key")
    @Nullable
    private String accessKey;

    @Column(name = "secret_key")
    @Nullable
    private String secretKey;

    @Column(name = "path_style_access", nullable = false)
    @Builder.Default
    private Boolean pathStyleAccess = true;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UuidCreatorUtils.generateUuidV7();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
