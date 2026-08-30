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
@Table(name = "nas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Nas {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 128)
    private String nasname; // IP ou FQDN do BNG

    @Column(length = 32)
    private @Nullable String shortname;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String type = "other";

    private @Nullable Integer ports;

    @Column(nullable = false, length = 64)
    private String secret;

    @Column(length = 64)
    private @Nullable String server;

    @Column(length = 64)
    private @Nullable String community;

    @Column(length = 200)
    private @Nullable String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "vendor_type", nullable = false, length = 50)
    @Builder.Default
    private NasVendorType vendorType = NasVendorType.MIKROTIK;

    @Column(name = "company_id")
    private @Nullable UUID companyId;

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
