package br.dev.xb.isperp.entity;

import br.dev.xb.isperp.signature.SignatureStatus;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "contract_signatures")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractSignature {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "contract_id", nullable = false)
    private UUID contractId;

    @Column(name = "template_id")
    private @Nullable UUID templateId;

    @Column(nullable = false, unique = true, length = 100)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private SignatureStatus status = SignatureStatus.PENDING;

    @Column(name = "symbolic_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal symbolicAmount = BigDecimal.valueOf(1.00);

    @Column(name = "pix_txid", length = 100)
    private @Nullable String pixTxid;

    @Column(name = "pix_end_to_end_id", length = 100)
    private @Nullable String pixEndToEndId;

    @Column(name = "pix_copy_paste", columnDefinition = "TEXT")
    private @Nullable String pixCopyPaste;

    @Column(name = "pix_qr_code_base64", columnDefinition = "TEXT")
    private @Nullable String pixQrCodeBase64;

    @Column(name = "rendered_content_snapshot", columnDefinition = "TEXT")
    private @Nullable String renderedContentSnapshot;

    @Column(name = "document_sha256_hash", length = 64)
    private @Nullable String documentSha256Hash;

    @Column(name = "client_ip", length = 50)
    private @Nullable String clientIp;

    @Column(name = "client_user_agent", columnDefinition = "TEXT")
    private @Nullable String clientUserAgent;

    @Column(name = "client_geo_latitude", precision = 10, scale = 8)
    private @Nullable BigDecimal clientGeoLatitude;

    @Column(name = "client_geo_longitude", precision = 11, scale = 8)
    private @Nullable BigDecimal clientGeoLongitude;

    @Column(name = "payer_name", length = 255)
    private @Nullable String payerName;

    @Column(name = "payer_cpf_cnpj", length = 20)
    private @Nullable String payerCpfCnpj;

    @Column(name = "payer_bank_name", length = 100)
    private @Nullable String payerBankName;

    @Column(name = "payer_bank_ispb", length = 20)
    private @Nullable String payerBankIspb;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private @Nullable String rejectionReason;

    @Column(name = "signed_pdf_url", columnDefinition = "TEXT")
    private @Nullable String signedPdfUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "fallback_method", nullable = false, length = 50)
    @Builder.Default
    private br.dev.xb.isperp.signature.FallbackMethod fallbackMethod = br.dev.xb.isperp.signature.FallbackMethod.PIX;

    @Column(name = "discount_applied_invoice_id")
    private @Nullable UUID discountAppliedInvoiceId;

    @Column(name = "onboarding_credit_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal onboardingCreditAmount = BigDecimal.ZERO;

    @Column(name = "forensic_certificate_pdf_url", columnDefinition = "TEXT")
    private @Nullable String forensicCertificatePdfUrl;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "signed_at")
    private @Nullable OffsetDateTime signedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UuidCreator.getTimeOrderedEpoch();
        }
        if (this.status == null) {
            this.status = SignatureStatus.PENDING;
        }
        if (this.symbolicAmount == null) {
            this.symbolicAmount = BigDecimal.valueOf(1.00);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContractSignature that = (ContractSignature) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
