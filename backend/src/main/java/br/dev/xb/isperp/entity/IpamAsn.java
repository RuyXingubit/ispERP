package br.dev.xb.isperp.entity;

import br.dev.xb.isperp.ipam.IpamRir;
import jakarta.persistence.*;
import lombok.*;
import org.jspecify.annotations.Nullable;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@EntityListeners(EntityLifecycleListener.class)
@Table(name = "ipam_asns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IpamAsn implements OffsetAuditableEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "company_id")
    private @Nullable UUID companyId;

    @Column(nullable = false, unique = true)
    private Long asn;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private IpamRir rir = IpamRir.REGISTRO_BR;

    @Column(columnDefinition = "TEXT")
    private @Nullable String description;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
