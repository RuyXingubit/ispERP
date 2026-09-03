package br.dev.xb.isperp.dto.backup;

import br.dev.xb.isperp.backup.SecurityMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackupOverviewDto {
    private SecurityMode securityMode;
    private boolean hasActivePolicy;
    private String cronExpression;
    private int retentionDays;
    private int activeDestinationsCount;
    private long totalBackupsCount;

    @Nullable
    private String lastBackupStatus;
    @Nullable
    private OffsetDateTime lastBackupAt;
    @Nullable
    private Long lastBackupSizeBytes;
    @Nullable
    private BigDecimal lastBackupCompressionRatio;
    @Nullable
    private String lastBackupSha256;
    @Nullable
    private String lastBackupFileName;

    private boolean isDryRunVerified;
    @Nullable
    private OffsetDateTime lastDryRunVerifiedAt;

    private boolean rescueKitDownloaded;
}
