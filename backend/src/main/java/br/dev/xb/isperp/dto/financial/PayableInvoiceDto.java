package br.dev.xb.isperp.dto.financial;

import br.dev.xb.isperp.entity.financial.PayableStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayableInvoiceDto {
    private UUID id;
    private String supplierName;
    private String supplierDocument;
    private UUID chartOfAccountId;
    private String chartOfAccountCode;
    private String chartOfAccountName;
    private String description;
    private String invoiceNumber;
    private BigDecimal totalAmount;
    private LocalDate issueDate;
    private PayableStatus status;
    private String notes;

    @Builder.Default
    private List<ExpenseInstallmentDto> installments = new ArrayList<>();
}
