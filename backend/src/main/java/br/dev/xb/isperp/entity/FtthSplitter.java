package br.dev.xb.isperp.entity;

import br.dev.xb.isperp.ftth.FtthSplitterType;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "ftth_splitters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FtthSplitter {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "company_id")
    private @Nullable UUID companyId;

    @Column(name = "closure_id")
    private @Nullable UUID closureId;

    @Column(nullable = false, length = 100)
    private String name;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "splitter_type", nullable = false, length = 50)
    private FtthSplitterType splitterType = FtthSplitterType.BALANCED_1_8;

    @Column(name = "input_cable_id")
    private @Nullable UUID inputCableId;

    @Column(name = "input_fiber_number")
    private @Nullable Integer inputFiberNumber;

    @Builder.Default
    @Column(name = "attenuation_db", nullable = false, precision = 4, scale = 2)
    private BigDecimal attenuationDb = new BigDecimal("10.50");

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

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
        FtthSplitter that = (FtthSplitter) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
