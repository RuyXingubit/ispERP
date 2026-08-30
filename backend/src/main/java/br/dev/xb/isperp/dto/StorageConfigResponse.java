package br.dev.xb.isperp.dto;

import br.dev.xb.isperp.storage.StorageProvider;
import br.dev.xb.isperp.storage.StorageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SuppressWarnings("null")
public class StorageConfigResponse {

    @Nullable
    private UUID id;

    @Nullable
    private UUID companyId;

    private StorageType storageType;

    private StorageProvider provider;

    @Nullable
    private String endpointUrl;

    private String bucketName;

    private String region;

    @Nullable
    private String accessKey;

    @Nullable
    private String maskedSecretKey;

    private Boolean pathStyleAccess;

    private Boolean isActive;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
