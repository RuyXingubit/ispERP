package br.dev.xb.isperp.fiscal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NfcomIssueRequest {
    private UUID invoiceId;
    @Nullable
    private UUID contractId;
    private UUID customerId;
    private String customerName;
    private String customerDocument; // CPF ou CNPJ
    @Nullable
    private String customerEmail;
    @Nullable
    private String customerPhone;
    private String customerStreet;
    private String customerNumber;
    private String customerNeighborhood;
    private String customerCity;
    private String customerState;
    private String customerZipCode;
    private String customerIbgeCode;

    private BigDecimal totalAmount;
    private LocalDate dueDate;
    private String description;
    @Nullable
    private List<NfcomItemDTO> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NfcomItemDTO {
        private String description;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
        private String cnae;
        private String cfop;
        private String classificationCode; // Ex: "01.01.01"
    }
}
