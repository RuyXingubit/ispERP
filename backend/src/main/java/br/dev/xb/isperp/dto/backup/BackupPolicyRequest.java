package br.dev.xb.isperp.dto.backup;

import br.dev.xb.isperp.backup.CompressionAlgorithm;
import br.dev.xb.isperp.backup.SecurityMode;
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
public class BackupPolicyRequest {

    @NotNull
    private SecurityMode securityMode;

    @Nullable
    private String customMasterKey;

    @NotBlank
    @Builder.Default
    private String cronExpression = "0 0 3 * * *";

    @Builder.Default
    private int retentionDays = 30;

    @NotNull
    @Builder.Default
    private CompressionAlgorithm compressionAlgorithm = CompressionAlgorithm.ZSTD;

    @Builder.Default
    private boolean autoDryRunEnabled = true;
}
