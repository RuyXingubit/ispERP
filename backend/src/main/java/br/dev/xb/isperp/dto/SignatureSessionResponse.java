package br.dev.xb.isperp.dto;

import br.dev.xb.isperp.signature.FallbackMethod;
import br.dev.xb.isperp.signature.SignatureStatus;
import lombok.*;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignatureSessionResponse {

    private UUID id;
    private UUID contractId;
    private @Nullable UUID templateId;
    private String token;
    private String signatureUrl;
    private SignatureStatus status;
    private BigDecimal symbolicAmount;
    private @Nullable String pixTxid;
    private @Nullable String pixCopyPaste;
    private @Nullable String pixQrCodeBase64;
    private @Nullable String pixEndToEndId;
    private @Nullable String documentSha256Hash;
    private @Nullable String payerName;
    private @Nullable String payerCpfCnpj;
    private @Nullable String payerBankName;
    private @Nullable String rejectionReason;
    private @Nullable String signedPdfUrl;
    private @Nullable FallbackMethod fallbackMethod;
    private @Nullable BigDecimal onboardingCreditAmount;
    private @Nullable UUID discountAppliedInvoiceId;
    private @Nullable String forensicCertificatePdfUrl;
    private OffsetDateTime expiresAt;
    private @Nullable OffsetDateTime signedAt;
    private OffsetDateTime createdAt;
}
