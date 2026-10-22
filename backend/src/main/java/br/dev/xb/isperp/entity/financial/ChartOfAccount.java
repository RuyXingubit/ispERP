package br.dev.xb.isperp.entity.financial;

import br.dev.xb.isperp.entity.EntityLifecycleListener;
import br.dev.xb.isperp.entity.OffsetAuditableEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@EntityListeners(EntityLifecycleListener.class)
@Table(name = "chart_of_accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChartOfAccount implements OffsetAuditableEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private ChartOfAccount parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = false)
    @Builder.Default
    private List<ChartOfAccount> children = new ArrayList<>();

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 30)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(name = "dre_category", nullable = false, length = 50)
    private DreCategory dreCategory;

    @Column(name = "is_synthetic", nullable = false)
    @Builder.Default
    private Boolean isSynthetic = false;

    @Column(name = "is_analytical", nullable = false)
    @Builder.Default
    private Boolean isAnalytical = true;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    @Override
    public void initEntityDefaults() {
        if (this.active == null) {
            this.active = true;
        }
    }
}
