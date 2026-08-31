package br.dev.xb.isperp.entity;

import br.dev.xb.isperp.monitoring.IncidentSeverity;
import br.dev.xb.isperp.monitoring.IncidentStatus;
import br.dev.xb.isperp.monitoring.IncidentType;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ftth_incidents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FtthIncident {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "company_id")
    private @Nullable UUID companyId;

    @Column(name = "network_device_id")
    private @Nullable UUID networkDeviceId;

    @Column(name = "olt_pon_port_id")
    private @Nullable UUID oltPonPortId;

    @Enumerated(EnumType.STRING)
    @Column(name = "incident_type", nullable = false, length = 50)
    private IncidentType incidentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    @Builder.Default
    private IncidentSeverity severity = IncidentSeverity.CRITICAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private IncidentStatus status = IncidentStatus.ACTIVE;

    @NotBlank(message = "Título do incidente é obrigatório")
    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private @Nullable String description;

    @Column(name = "affected_customers_count", nullable = false)
    @Builder.Default
    private int affectedCustomersCount = 0;

    @Column(name = "affected_ctos_ids", columnDefinition = "jsonb")
    private @Nullable String affectedCtosIds; // JSON array de UUIDs

    @Column(name = "affected_cable_id")
    private @Nullable UUID affectedCableId;

    @Column(name = "estimated_cut_latitude", precision = 10, scale = 8)
    private @Nullable BigDecimal estimatedCutLatitude;

    @Column(name = "estimated_cut_longitude", precision = 11, scale = 8)
    private @Nullable BigDecimal estimatedCutLongitude;

    @Column(name = "estimated_cut_details")
    private @Nullable String estimatedCutDetails;

    @Column(name = "work_order_id")
    private @Nullable UUID workOrderId;

    @Column(name = "detected_at", nullable = false)
    private OffsetDateTime detectedAt;

    @Column(name = "dispatched_at")
    private @Nullable OffsetDateTime dispatchedAt;

    @Column(name = "resolved_at")
    private @Nullable OffsetDateTime resolvedAt;

    @Column(name = "root_cause_notes", columnDefinition = "TEXT")
    private @Nullable String rootCauseNotes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UuidCreatorUtils.generateUuidV7();
        }
        if (this.detectedAt == null) {
            this.detectedAt = OffsetDateTime.now();
        }
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
