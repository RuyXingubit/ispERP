package br.dev.xb.isperp.dto.financial;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayableInvoiceRequest {

    @NotBlank(message = "Nome do fornecedor é obrigatório")
    private String supplierName;

    private String supplierDocument;

    @NotNull(message = "Conta contábil é obrigatória")
    private UUID chartOfAccountId;

    @NotBlank(message = "Descrição é obrigatória")
    private String description;

    private String invoiceNumber;

    @NotNull(message = "Valor total é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
    private BigDecimal totalAmount;

    @Builder.Default
    private LocalDate issueDate = LocalDate.now();

    @NotNull(message = "Quantidade de parcelas é obrigatória")
    @Min(value = 1, message = "Deve conter ao menos 1 parcela")
    @Builder.Default
    private Integer installmentsCount = 1;

    @NotNull(message = "Data de vencimento da primeira parcela é obrigatória")
    private LocalDate firstDueDate;

    private String notes;
}
