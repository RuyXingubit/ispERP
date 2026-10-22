package br.dev.xb.isperp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "plan_composition_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanCompositionItem implements CompositionItemView {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Plan plan;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "fiscal_code", length = 30)
    private String fiscalCode;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 40)
    @Builder.Default
    private PlanCompositionCategory category = PlanCompositionCategory.SCM;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @PrePersist
    protected void onCreate() {
        initDefaults();
    }
}
