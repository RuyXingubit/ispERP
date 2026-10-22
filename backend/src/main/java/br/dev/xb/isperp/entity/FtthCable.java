package br.dev.xb.isperp.entity;

import br.dev.xb.isperp.ftth.FiberColorStandard;
import br.dev.xb.isperp.ftth.FtthCableType;
import jakarta.persistence.*;
import lombok.*;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@EntityListeners(EntityLifecycleListener.class)
@Table(name = "ftth_cables")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class FtthCable implements OffsetAuditableEntity {

    @Id
    @EqualsAndHashCode.Include
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
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
