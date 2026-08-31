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
@Table(name = "ftth_ctos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FtthCto {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "company_id")
    private @Nullable UUID companyId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "pole_id")
    private @Nullable UUID poleId;

    @Column(name = "closure_id")
    private @Nullable UUID closureId; // Caixa de emenda alimentadora

    @Column(nullable = false, precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 11, scale = 8)
    private BigDecimal longitude;

    @Builder.Default
    @Column(name = "total_ports", nullable = false)
    private int totalPorts = 16;

    @Builder.Default
    @Column(name = "splitter_type", nullable = false, length = 50)
    private String splitterType = "BALANCED_1_16";

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String status = "ATIVA";

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
        FtthCto that = (FtthCto) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
