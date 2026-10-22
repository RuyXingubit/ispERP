package br.dev.xb.isperp.entity;

import jakarta.persistence.*;
import lombok.*;
import org.jspecify.annotations.Nullable;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@EntityListeners(EntityLifecycleListener.class)
@Table(name = "ipam_vrfs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IpamVrf implements OffsetAuditableEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "company_id")
    private @Nullable UUID companyId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    private @Nullable String rd; // Route Distinguisher

    @Column(columnDefinition = "TEXT")
    private @Nullable String description;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private boolean isDefault = false;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
