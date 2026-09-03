package br.dev.xb.isperp.dto.financial;

import br.dev.xb.isperp.entity.financial.PayableStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseInstallmentDto {
    private UUID id;
    private Integer installmentNumber;
    private Integer totalInstallments;
    private LocalDate dueDate;
    private BigDecimal amount;
    private BigDecimal interestAmount;
    private PayableStatus status;
    private OffsetDateTime paidAt;
    private BigDecimal paidAmount;
    private String paymentMethod;
    private String receiptUrl;
}
