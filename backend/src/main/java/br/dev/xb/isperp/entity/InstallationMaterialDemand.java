package br.dev.xb.isperp.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.jspecify.annotations.Nullable;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "installation_material_demands")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstallationMaterialDemand {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "work_order_id", nullable = false)
    private UUID workOrderId;

    @Column(name = "contract_id", nullable = false)
    private UUID contractId;

    @Column(name = "cto_id")
    private @Nullable UUID ctoId;

    @Column(name = "cto_port_number")
    private @Nullable Integer ctoPortNumber;

    @Column(name = "estimated_drop_meters", nullable = false)
    @Builder.Default
    private Integer estimatedDropMeters = 50;

    @Column(name = "onu_model_required", nullable = false, length = 100)
    @Builder.Default
    private String onuModelRequired = "ONT Wi-Fi Dual-Band GPON";

    @Column(name = "fast_connectors_count", nullable = false)
    @Builder.Default
    private Integer fastConnectorsCount = 2;

    @Column(name = "pto_rosette_count", nullable = false)
    @Builder.Default
    private Integer ptoRosetteCount = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private MaterialDemandStatus status = MaterialDemandStatus.PENDING_ALLOCATION;

    @Column(name = "allocated_warehouse_id")
    private @Nullable UUID allocatedWarehouseId;

    @Column(name = "allocated_technician_name", length = 150)
    private @Nullable String allocatedTechnicianName;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UuidCreator.getTimeOrderedEpoch();
        }
        if (this.status == null) {
            this.status = MaterialDemandStatus.PENDING_ALLOCATION;
        }
        if (this.estimatedDropMeters == null) {
            this.estimatedDropMeters = 50;
        }
        if (this.fastConnectorsCount == null) {
            this.fastConnectorsCount = 2;
        }
        if (this.ptoRosetteCount == null) {
            this.ptoRosetteCount = 1;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InstallationMaterialDemand that = (InstallationMaterialDemand) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
