package br.dev.xb.isperp.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateChargeResponse {

    private String externalTransactionId;
    private String gatewayType;
    private UUID gatewayConfigId;
    private BigDecimal amount;
    private String status;
    private String pixCopiaECola;
    private String pixQrCodeUrl;
    private String barcode;
    private String digitableLine;
    private String pdfUrl;
}
