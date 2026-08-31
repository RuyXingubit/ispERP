package br.dev.xb.isperp.dto;

import lombok.*;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PixSignatureWebhookRequest {

    private String txid;
    private String endToEndId;
    private BigDecimal amount;
    private String payerName;
    private String payerCpfCnpj;
    private @Nullable String bankName;
    private @Nullable String ispb;
}
