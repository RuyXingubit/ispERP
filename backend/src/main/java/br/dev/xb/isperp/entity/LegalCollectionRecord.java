package br.dev.xb.isperp.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@EntityListeners(EntityLifecycleListener.class)
@Table(name = "legal_collection_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LegalCollectionRecord implements LocalAuditableEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull(message = "Cliente é obrigatório")
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @NotNull(message = "Contrato é obrigatório")
    @Column(name = "contract_id", nullable = false)
    private UUID contractId;

    @Nullable
    @Column(name = "work_order_id")
    private UUID workOrderId;

    @NotNull
    @Column(name = "total_debt_invoices", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalDebtInvoices = BigDecimal.ZERO;

    @NotNull
    @Column(name = "equipment_indemnity_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal equipmentIndemnityAmount = BigDecimal.ZERO;

    @NotNull
    @Column(name = "total_claim_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalClaimAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private LegalCollectionStatus status = LegalCollectionStatus.PENDING_BUREAU_SUBMISSION;

    @Nullable
    @Column(name = "evidence_notes", columnDefinition = "text")
    private String evidenceNotes;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private @Nullable LocalDateTime updatedAt;
    @Override
    public void initEntityDefaults() {
        if (this.totalClaimAmount == null || this.totalClaimAmount.compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal debt = this.totalDebtInvoices != null ? this.totalDebtInvoices : BigDecimal.ZERO;
            BigDecimal equip = this.equipmentIndemnityAmount != null ? this.equipmentIndemnityAmount : BigDecimal.ZERO;
            this.totalClaimAmount = debt.add(equip);
        }
        if (this.status == null) {
            this.status = LegalCollectionStatus.PENDING_BUREAU_SUBMISSION;
        }
    }


    public enum LegalCollectionStatus {
        PENDING_BUREAU_SUBMISSION,
        SENT_TO_SERASA_SPC,
        IN_LEGAL_LITIGATION,
        RESOLVED
    }
}
