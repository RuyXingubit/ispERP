package br.dev.xb.isperp.dto.financial;

import br.dev.xb.isperp.entity.financial.SentinelSeverity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SentinelAuditLogDto {
    private UUID id;
    private String auditType;
    private SentinelSeverity severity;
    private String title;
    private String description;
    private String geminiAnalysis;
    private String recommendedAction;
    private Boolean resolved;
    private OffsetDateTime createdAt;
}
