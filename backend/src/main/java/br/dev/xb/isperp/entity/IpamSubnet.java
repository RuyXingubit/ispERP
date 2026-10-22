package br.dev.xb.isperp.entity;

import br.dev.xb.isperp.ipam.IpamIpVersion;
import br.dev.xb.isperp.ipam.IpamSubnetCategory;
import br.dev.xb.isperp.ipam.IpamSubnetStatus;
import jakarta.persistence.*;
import lombok.*;
import org.jspecify.annotations.Nullable;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@EntityListeners(EntityLifecycleListener.class)
@Table(name = "ipam_subnets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IpamSubnet implements OffsetAuditableEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private @Nullable IpamSubnet parent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vrf_id")
    private @Nullable IpamVrf vrf;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asn_id")
    private @Nullable IpamAsn asn;

    @Column(name = "company_id")
    private @Nullable UUID companyId;

    @Column(nullable = false, length = 64)
    private String cidr;

    @Enumerated(EnumType.STRING)
    @Column(name = "ip_version", nullable = false, length = 4)
    private IpamIpVersion ipVersion;

    @Column(name = "network_address", nullable = false, length = 45)
    private String networkAddress;

    @Column(name = "broadcast_address", length = 45)
    private @Nullable String broadcastAddress;

    @Column(name = "prefix_length", nullable = false)
    private Integer prefixLength;

    @Column(name = "total_hosts", nullable = false)
    @Builder.Default
    private Long totalHosts = 0L;

    @Column(name = "is_pool", nullable = false)
    @Builder.Default
    private boolean isPool = false;

    @Column(name = "pool_name", length = 100)
    private @Nullable String poolName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private IpamSubnetStatus status = IpamSubnetStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private IpamSubnetCategory category = IpamSubnetCategory.CUSTOMER_ACCESS;

    @Column(columnDefinition = "TEXT")
    private @Nullable String description;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
