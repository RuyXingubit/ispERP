package br.dev.xb.isperp.entity.financial;

import br.dev.xb.isperp.util.UuidCreatorUtils;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "sentinel_audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SentinelAuditLog {

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

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UuidCreatorUtils.generateUuidV7();
        }
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
        if (this.resolved == null) {
            this.resolved = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
