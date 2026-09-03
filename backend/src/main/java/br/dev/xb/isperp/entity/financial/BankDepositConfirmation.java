package br.dev.xb.isperp.entity.financial;

import br.dev.xb.isperp.entity.User;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "bank_deposit_confirmations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankDepositConfirmation {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depositor_user_id", nullable = false)
    private User depositor;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "bank_name", nullable = false, length = 100)
    private String bankName;

    @Column(name = "bank_agency", length = 50)
    private String bankAgency;

    @Column(name = "bank_account", length = 50)
    private String bankAccount;

    @Column(name = "receipt_file_url", nullable = false, length = 500)
    private String receiptFileUrl;

    @Column(name = "deposit_date", nullable = false)
    @Builder.Default
    private OffsetDateTime depositDate = OffsetDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private BankDepositStatus status = BankDepositStatus.PENDING_AUDIT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audited_by_user_id")
    private User auditedBy;

    @Column(name = "audited_at")
    private OffsetDateTime auditedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UuidCreatorUtils.generateUuidV7();
        }
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
        if (this.depositDate == null) {
            this.depositDate = OffsetDateTime.now();
        }
        if (this.status == null) {
            this.status = BankDepositStatus.PENDING_AUDIT;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
