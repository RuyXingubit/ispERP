package br.dev.xb.isperp.entity;

import br.dev.xb.isperp.radius.RadiusBlockMode;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "radius_policy_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RadiusPolicyConfig {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Builder.Default
    @Column(name = "auto_block_enabled", nullable = false)
    private boolean autoBlockEnabled = true;

    @Builder.Default
    @Column(name = "tolerance_days", nullable = false)
    private int toleranceDays = 5;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "block_mode", nullable = false, length = 30)
    private RadiusBlockMode blockMode = RadiusBlockMode.CAPTIVE_PORTAL;

    @Builder.Default
    @Column(name = "reduced_download_kbps", nullable = false)
    private int reducedDownloadKbps = 256;

    @Builder.Default
    @Column(name = "reduced_upload_kbps", nullable = false)
    private int reducedUploadKbps = 256;

    @Builder.Default
    @Column(name = "unblock_on_payment", nullable = false)
    private boolean unblockOnPayment = true;

    @Builder.Default
    @Column(name = "send_pod_on_block", nullable = false)
    private boolean sendPodOnBlock = true;

    @Builder.Default
    @Column(name = "send_pod_on_unblock", nullable = false)
    private boolean sendPodOnUnblock = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
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
        RadiusPolicyConfig that = (RadiusPolicyConfig) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
