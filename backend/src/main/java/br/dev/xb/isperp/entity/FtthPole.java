package br.dev.xb.isperp.entity;

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
@Table(name = "ftth_poles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FtthPole {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "company_id")
    private @Nullable UUID companyId;

    @Column(nullable = false, length = 50)
    private String code; // P-10492

    @Column(nullable = false, precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 11, scale = 8)
    private BigDecimal longitude;

    @Builder.Default
    @Column(name = "pole_type", nullable = false, length = 50)
    private String poleType = "CONCRETO";

    @Builder.Default
    @Column(name = "reservation_meters", nullable = false)
    private int reservationMeters = 0;

    @Column(columnDefinition = "TEXT")
    private @Nullable String description;

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
        FtthPole ftthPole = (FtthPole) o;
        return Objects.equals(id, ftthPole.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
