package br.dev.xb.isperp.dto.backup;

import br.dev.xb.isperp.backup.StorageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackupDestinationRequest {

    @NotBlank
    private String name;

    @NotNull
    private StorageType storageType;

    @Nullable
    private String endpointUrl;

    @Nullable
    private String bucketName;

    @Builder.Default
    private String region = "auto";

    @Nullable
    private String accessKey;

    @Nullable
    private String secretKey;

    @Builder.Default
    private String pathPrefix = "backups/isperp";

    @Builder.Default
    private boolean isPrimary = false;
}
