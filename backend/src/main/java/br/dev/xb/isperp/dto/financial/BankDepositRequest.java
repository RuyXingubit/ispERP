package br.dev.xb.isperp.dto.financial;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankDepositRequest {

    @NotNull(message = "Valor depositado é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
    private BigDecimal amount;

    @NotBlank(message = "Nome do banco é obrigatório")
    private String bankName;

    private String bankAgency;
    private String bankAccount;

    @NotBlank(message = "Comprovante de depósito é obrigatório")
    private String receiptFileUrl;

    private String notes;
}
