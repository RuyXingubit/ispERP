package br.dev.xb.isperp.dto.backup;

import br.dev.xb.isperp.backup.StorageType;
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
public class BackupDestinationResponse {
    private UUID id;
    private String name;
    private StorageType storageType;
    @Nullable
    private String endpointUrl;
    @Nullable
    private String bucketName;
    private String region;
    @Nullable
    private String accessKey;
    private String pathPrefix;
    private boolean isActive;
    private boolean isPrimary;
    @Nullable
    private OffsetDateTime lastTestedAt;
    @Nullable
    private String lastTestStatus;
    @Nullable
    private String lastTestError;
    private OffsetDateTime createdAt;
}
