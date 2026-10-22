package br.dev.xb.isperp.entity;

import br.dev.xb.isperp.util.UuidCreatorUtils;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "plans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SuppressWarnings("java:S2583")
public class Plan {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotBlank(message = "Nome do plano é obrigatório")
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @NotNull(message = "Velocidade de download é obrigatória")
    @Positive(message = "Velocidade de download deve ser positiva")
    @Column(name = "download_speed", nullable = false)
    private Integer downloadSpeed; // Mbps

    @NotNull(message = "Velocidade de upload é obrigatória")
    @Positive(message = "Velocidade de upload deve ser positiva")
    @Column(name = "upload_speed", nullable = false)
    private Integer uploadSpeed; // Mbps

    @NotNull(message = "Preço é obrigatório")
    @Positive(message = "Preço deve ser positivo")
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "scm_amount", precision = 10, scale = 2)
    private BigDecimal scmAmount;

    @Column(name = "sva_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal svaAmount = BigDecimal.ZERO;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<PlanCompositionItem> compositionItems = new ArrayList<>();

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "sva_included", length = 255)
    private String svaIncluded;

    @Column(name = "suspension_days")
    @Builder.Default
    private Integer suspensionDays = 5;

    @Column(name = "always_issue_nfcom", nullable = false)
    @Builder.Default
    private Boolean alwaysIssueNfcom = false;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void recalculateCompositionTotals() {
        if (this.compositionItems == null || this.compositionItems.isEmpty()) {
            this.scmAmount = this.price != null ? this.price : BigDecimal.ZERO;
            this.svaAmount = BigDecimal.ZERO;
            return;
        }
        this.compositionItems.forEach(item -> item.setPlan(this));
        BigDecimal[] totals = CompositionItemView.calculateScmAndSvaTotals(this.compositionItems);
        this.scmAmount = totals[0];
        this.svaAmount = totals[1];
    }

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UuidCreatorUtils.generateUuidV7();
        }
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.active == null) {
            this.active = true;
        }
        recalculateCompositionTotals();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        recalculateCompositionTotals();
    }
}

