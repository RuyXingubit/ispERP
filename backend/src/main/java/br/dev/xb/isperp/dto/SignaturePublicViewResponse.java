package br.dev.xb.isperp.dto;

import br.dev.xb.isperp.signature.FallbackMethod;
import br.dev.xb.isperp.signature.SignatureStatus;
import lombok.*;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignaturePublicViewResponse {

    private String token;
    private String contractName;
    private String customerName;
    private String customerDocumentMasked;
    private String companyName;
    private String renderedContent;
    private String consentClause;
    private SignatureStatus status;
    private BigDecimal symbolicAmount;
    private @Nullable String pixCopyPaste;
    private @Nullable String pixQrCodeBase64;
    private @Nullable String payerName;
    private @Nullable String payerBankName;
    private @Nullable String rejectionReason;
    private @Nullable String signedPdfUrl;
    private @Nullable String documentSha256Hash;
    private @Nullable FallbackMethod fallbackMethod;
    private @Nullable BigDecimal onboardingCreditAmount;
    private @Nullable String forensicCertificatePdfUrl;
    private OffsetDateTime expiresAt;
    private @Nullable OffsetDateTime signedAt;
}
