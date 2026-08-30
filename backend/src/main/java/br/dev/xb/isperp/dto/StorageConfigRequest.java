package br.dev.xb.isperp.dto;

import br.dev.xb.isperp.storage.StorageProvider;
import br.dev.xb.isperp.storage.StorageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SuppressWarnings("null")
public class StorageConfigRequest {

    @Nullable
    private UUID companyId;

    @NotNull(message = "O tipo de storage é obrigatório")
    @Builder.Default
    private StorageType storageType = StorageType.S3;

    @NotNull(message = "O provedor é obrigatório")
    @Builder.Default
    private StorageProvider provider = StorageProvider.SEAWEEDFS_LOCAL;

    @Nullable
    private String endpointUrl;

    @NotBlank(message = "O nome do bucket é obrigatório")
    @Builder.Default
    private String bucketName = "isperp-files";

    @NotBlank(message = "A região é obrigatória")
    @Builder.Default
    private String region = "us-east-1";

    @Nullable
    private String accessKey;

    @Nullable
    private String secretKey;

    @NotNull(message = "Definição de path-style access é obrigatória")
    @Builder.Default
    private Boolean pathStyleAccess = true;

    @NotNull(message = "Status ativo/inativo é obrigatório")
    @Builder.Default
    private Boolean isActive = true;
}
