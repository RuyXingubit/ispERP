package br.dev.xb.isperp.entity;

import br.dev.xb.isperp.fiscal.FiscalRegimeTransitionStatus;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "fiscal_regime_transitions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SuppressWarnings("null")
public class FiscalRegimeTransition {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @Nullable
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "previous_regime", nullable = false, length = 30)
    private String previousRegime;

    @Column(name = "new_regime", nullable = false, length = 30)
    private String newRegime;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "aliquota_icms", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal aliquotaIcms = BigDecimal.ZERO;

    @Column(name = "aliquota_pis", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal aliquotaPis = BigDecimal.ZERO;

    @Column(name = "aliquota_cofins", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal aliquotaCofins = BigDecimal.ZERO;

    @Column(name = "aliquota_fust", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal aliquotaFust = new BigDecimal("0.65");

    @Column(name = "aliquota_funttel", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal aliquotaFunttel = new BigDecimal("0.50");

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private FiscalRegimeTransitionStatus status = FiscalRegimeTransitionStatus.SCHEDULED;

    @Column(name = "notes")
    @Nullable
    private String notes;

    @Column(name = "applied_at")
    @Nullable
    private LocalDateTime appliedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UuidCreatorUtils.generateUuidV7();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
