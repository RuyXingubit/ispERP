package br.dev.xb.isperp.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateChargeRequest {

    private UUID invoiceId;
    private UUID contractId;
    private UUID customerId;
    private String customerName;
    private String customerCpf;
    private String customerEmail;
    private String customerPhone;
    private BigDecimal amount;
    private LocalDate dueDate;
    private String description;
    
    // Configurações de Multa e Juros (COBV)
    @Builder.Default
    private BigDecimal penaltyPercentage = new BigDecimal("2.00"); // 2% multa
    
    @Builder.Default
    private BigDecimal interestMonthlyPercentage = new BigDecimal("1.00"); // 1% juros/mês
}
