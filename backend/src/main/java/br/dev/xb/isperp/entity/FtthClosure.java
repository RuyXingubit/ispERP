package br.dev.xb.isperp.entity;

import br.dev.xb.isperp.ftth.FtthClosureType;
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
@Table(name = "ftth_closures")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FtthClosure {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "company_id")
    private @Nullable UUID companyId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "pole_id")
    private @Nullable UUID poleId;

    @Column(precision = 10, scale = 8)
    private @Nullable BigDecimal latitude;

    @Column(precision = 11, scale = 8)
    private @Nullable BigDecimal longitude;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "closure_type", nullable = false, length = 50)
    private FtthClosureType closureType = FtthClosureType.DOMO;

    @Builder.Default
    @Column(name = "tray_count", nullable = false)
    private int trayCount = 4;

    @Builder.Default
    @Column(name = "capacity_fusions", nullable = false)
    private int capacityFusions = 48;

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String status = "ATIVA";

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
        FtthClosure that = (FtthClosure) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
