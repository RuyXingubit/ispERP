package br.dev.xb.isperp.entity;

import br.dev.xb.isperp.ftth.FtthPortStatus;
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
@Table(name = "ftth_cto_ports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FtthCtoPort {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "cto_id", nullable = false)
    private UUID ctoId;

    @Column(name = "port_number", nullable = false)
    private int portNumber;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FtthPortStatus status = FtthPortStatus.LIVRE;

    @Column(name = "onu_provisioning_id")
    private @Nullable UUID onuProvisioningId;

    @Column(name = "customer_id")
    private @Nullable UUID customerId;

    @Column(columnDefinition = "TEXT")
    private @Nullable String notes;

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
        FtthCtoPort that = (FtthCtoPort) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
