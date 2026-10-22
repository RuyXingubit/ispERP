package br.dev.xb.isperp.entity.financial;

import br.dev.xb.isperp.entity.EntityLifecycleListener;
import br.dev.xb.isperp.entity.OffsetAuditableEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@EntityListeners(EntityLifecycleListener.class)
@Table(name = "sentinel_audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SentinelAuditLog implements OffsetAuditableEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "audit_type", nullable = false, length = 100)
    private String auditType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 50)
    private SentinelSeverity severity;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "gemini_analysis", columnDefinition = "TEXT")
    private String geminiAnalysis;

    @Column(name = "recommended_action", columnDefinition = "TEXT")
    private String recommendedAction;

    @Column(name = "resolved", nullable = false)
    @Builder.Default
    private Boolean resolved = false;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    @Override
    public void initEntityDefaults() {
        if (this.resolved == null) {
            this.resolved = false;
        }
    }
}
