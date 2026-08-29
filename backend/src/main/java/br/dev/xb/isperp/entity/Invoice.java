package br.dev.xb.isperp.entity;

import br.dev.xb.isperp.util.UuidCreatorUtils;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
@SuppressWarnings("null")
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

    @Column(name = "gateway_config_id")
    private UUID gatewayConfigId;

    @Column(name = "gateway_type", nullable = false, length = 50)
    @Builder.Default
    private String gatewayType = "XINGUBIT_PAY";

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

    @Column(name = "pix_qrcode_url", columnDefinition = "text")
    private String pixQrCodeUrl;

    @Column(name = "pix_copia_e_cola", columnDefinition = "text")
    private String pixCopiaECola;

    @Column(name = "barcode", length = 100)
    private String barcode;

    @Column(name = "digitable_line", length = 100)
    private String digitableLine;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "paid_amount", precision = 10, scale = 2)
    private BigDecimal paidAmount;

    @Column(name = "payment_method", length = 30)
    private String paymentMethod;

    @Column(name = "pdf_url", columnDefinition = "text")
    private String pdfUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

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
}
