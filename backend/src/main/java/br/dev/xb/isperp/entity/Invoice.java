package br.dev.xb.isperp.entity;

import br.dev.xb.isperp.util.UuidCreatorUtils;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
@Table(name = "invoices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull(message = "ID do contrato é obrigatório")
    @Column(name = "contract_id", nullable = false)
    private UUID contractId;

    @NotNull(message = "ID do cliente é obrigatório")
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Nullable
    @Column(name = "gateway_config_id")
    private UUID gatewayConfigId;

    @Column(name = "gateway_type", nullable = false, length = 50)
    @Builder.Default
    private String gatewayType = "XINGUBIT_PAY";

    @Nullable
    @Column(name = "external_transaction_id", length = 100)
    private String externalTransactionId;

    @NotNull(message = "Valor da fatura é obrigatório")
    @Positive(message = "Valor da fatura deve ser positivo")
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "penalty_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal penaltyAmount = BigDecimal.ZERO;

    @Column(name = "interest_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal interestAmount = BigDecimal.ZERO;

    @NotNull(message = "Data de vencimento é obrigatória")
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.PENDING;

    @Nullable
    @Column(name = "pix_qrcode_url", columnDefinition = "text")
    private String pixQrCodeUrl;

    @Nullable
    @Column(name = "pix_copia_e_cola", columnDefinition = "text")
    private String pixCopiaECola;

    @Nullable
    @Column(name = "barcode", length = 100)
    private String barcode;

    @Nullable
    @Column(name = "digitable_line", length = 100)
    private String digitableLine;

    @Nullable
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Nullable
    @Column(name = "paid_amount", precision = 10, scale = 2)
    private BigDecimal paidAmount;

    @Nullable
    @Column(name = "payment_method", length = 30)
    private String paymentMethod;

    @Nullable
    @Column(name = "paid_by_cross_credit_id")
    private UUID paidByCrossCreditId;

    @Column(name = "protected_against_suspension", nullable = false)
    @Builder.Default
    private Boolean protectedAgainstSuspension = false;

    @Nullable
    @Column(name = "rebalance_notice", columnDefinition = "text")
    private String rebalanceNotice;

    @Nullable
    @Column(name = "pdf_url", columnDefinition = "text")
    private String pdfUrl;

    @Nullable
    @Column(name = "nfcom_number")
    private Integer nfcomNumber;

    @Nullable
    @Column(name = "nfcom_series")
    private Integer nfcomSeries;

    @Nullable
    @Column(name = "nfcom_key", length = 44)
    private String nfcomKey;

    @Nullable
    @Column(name = "nfcom_xml_url", length = 500)
    private String nfcomXmlUrl;

    @Nullable
    @Column(name = "nfcom_pdf_url", length = 500)
    private String nfcomPdfUrl;

    @Nullable
    @Column(name = "settled_in_cash_by_user_id")
    private UUID settledInCashByUserId;

    @Nullable
    @Column(name = "receipt_number", length = 50)
    private String receiptNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "nfcom_status", nullable = false, length = 30)
    @Builder.Default
    private NfcomStatus nfcomStatus = NfcomStatus.NOT_APPLICABLE;

    @Nullable
    @Column(name = "nfcom_issued_at")
    private LocalDateTime nfcomIssuedAt;

    @Nullable
    @Column(name = "nfcom_error_message", columnDefinition = "text")
    private String nfcomErrorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Nullable
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UuidCreatorUtils.generateUuidV7();
        }
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = InvoiceStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum InvoiceStatus {
        PENDING,
        PAID,
        OVERDUE,
        CANCELED
    }

    public enum NfcomStatus {
        NOT_APPLICABLE,
        PENDING_EMISSION,
        ISSUED,
        FAILED,
        CANCELLED
    }
}
