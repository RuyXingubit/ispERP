package br.dev.xb.isperp.entity;

import br.dev.xb.isperp.radius.NasVendorType;
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
@Table(name = "cgnat_mappings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CgnatMapping {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nas_id")
    private @Nullable Nas nas;

    @Enumerated(EnumType.STRING)
    @Column(name = "vendor_type", nullable = false, length = 50)
    @Builder.Default
    private NasVendorType vendorType = NasVendorType.MIKROTIK;

    @Column(name = "public_ip", nullable = false, length = 45)
    private String publicIp;

    @Column(name = "port_start", nullable = false)
    private Integer portStart;

    @Column(name = "port_end", nullable = false)
    private Integer portEnd;

    @Column(name = "private_ip_start", nullable = false, length = 45)
    private String privateIpStart;

    @Column(name = "private_ip_end", nullable = false, length = 45)
    private String privateIpEnd;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String protocol = "BOTH"; // TCP, UDP, BOTH

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
            this.id = Objects.requireNonNull(UuidCreator.getTimeOrderedEpoch());
        }
    }
}
