package br.dev.xb.isperp.entity;

import br.dev.xb.isperp.util.UuidCreatorUtils;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "contract_lifecycle_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractLifecycleEvent {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @Nullable
    private UUID id;

    @Column(name = "contract_id", nullable = false)
    private UUID contractId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private EventType eventType;

    @Nullable
    @Column(name = "previous_customer_id")
    private UUID previousCustomerId;

    @Nullable
    @Column(name = "new_customer_id")
    private UUID newCustomerId;

    @Nullable
    @Column(name = "previous_plan_id")
    private UUID previousPlanId;

    @Nullable
    @Column(name = "new_plan_id")
    private UUID newPlanId;

    @Nullable
    @Column(name = "previous_address", length = 255)
    private String previousAddress;

    @Nullable
    @Column(name = "new_address", length = 255)
    private String newAddress;

    @Nullable
    @Column(name = "old_monthly_fee", precision = 10, scale = 2)
    private BigDecimal oldMonthlyFee;

    @Nullable
    @Column(name = "new_monthly_fee", precision = 10, scale = 2)
    private BigDecimal newMonthlyFee;

    @Nullable
    @Column(name = "remaining_days_in_cycle")
    private Integer remainingDaysInCycle;

    @Nullable
    @Column(name = "pro_rata_adjustment_amount", precision = 10, scale = 2)
    private BigDecimal proRataAdjustmentAmount;

    @Nullable
    @Column(name = "adjusted_invoice_id")
    private UUID adjustedInvoiceId;

    @Nullable
    @Column(name = "migrated_invoices_count")
    private Integer migratedInvoicesCount;

    @Nullable
    @Column(name = "work_order_id")
    private UUID workOrderId;

    @Nullable
    @Column(name = "signature_session_id")
    private UUID signatureSessionId;

    @Nullable
    @Column(name = "signature_url", length = 500)
    private String signatureUrl;

    @Nullable
    @Column(name = "radius_rate_limit_applied", length = 120)
    private String radiusRateLimitApplied;

    @Nullable
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UuidCreatorUtils.generateUuidV7();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public enum EventType {
        PLAN_CHANGE,
        ADDRESS_CHANGE,
        OWNERSHIP_TRANSFER
    }
}
