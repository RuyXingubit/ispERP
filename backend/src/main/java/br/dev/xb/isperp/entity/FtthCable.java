package br.dev.xb.isperp.entity;

import br.dev.xb.isperp.ftth.FiberColorStandard;
import br.dev.xb.isperp.ftth.FtthCableType;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "ftth_cables")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FtthCable {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "company_id")
    private @Nullable UUID companyId;

    @Column(nullable = false, length = 150)
    private String name;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "cable_type", nullable = false, length = 50)
    private FtthCableType cableType = FtthCableType.DISTRIBUICAO;

    @Builder.Default
    @Column(name = "fiber_count", nullable = false)
    private int fiberCount = 12;

    @Builder.Default
    @Column(name = "tube_count", nullable = false)
    private int tubeCount = 1;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "color_standard", nullable = false, length = 30)
    private FiberColorStandard colorStandard = FiberColorStandard.ABNT_NBR_14106;

    @Builder.Default
    @Column(name = "length_meters", nullable = false, precision = 10, scale = 2)
    private BigDecimal lengthMeters = BigDecimal.ZERO;

    @Column(name = "path_coordinates", columnDefinition = "jsonb")
    private @Nullable String pathCoordinates; // GeoJSON string

    @Column(name = "source_pop_id")
    private @Nullable UUID sourcePopId;

    @Column(name = "source_pole_id")
    private @Nullable UUID sourcePoleId;

    @Column(name = "target_pole_id")
    private @Nullable UUID targetPoleId;

    @Builder.Default
    @Column(name = "attenuation_db_per_km", nullable = false, precision = 4, scale = 2)
    private BigDecimal attenuationDbPerKm = new BigDecimal("0.35");

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
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FtthCable ftthCable = (FtthCable) o;
        return Objects.equals(id, ftthCable.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
