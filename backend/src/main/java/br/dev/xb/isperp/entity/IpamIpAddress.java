package br.dev.xb.isperp.entity;

import br.dev.xb.isperp.ipam.IpamAddressStatus;
import br.dev.xb.isperp.ipam.IpamAssignedToType;
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
@Table(name = "ipam_ip_addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IpamIpAddress {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subnet_id", nullable = false)
    private IpamSubnet subnet;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private IpamAddressStatus status = IpamAddressStatus.AVAILABLE;

    @Enumerated(EnumType.STRING)
    @Column(name = "assigned_to_type", length = 50)
    private @Nullable IpamAssignedToType assignedToType;

    @Column(name = "assigned_to_id")
    private @Nullable UUID assignedToId;

    @Column(name = "dns_name", length = 255)
    private @Nullable String dnsName;

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
            this.id = Objects.requireNonNull(UuidCreator.getTimeOrderedEpoch());
        }
    }
}
