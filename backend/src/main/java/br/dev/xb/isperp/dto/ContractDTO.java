package br.dev.xb.isperp.dto;

import br.dev.xb.isperp.entity.Contract;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractDTO {

    private UUID id;
    private UUID customerId;
    private String customerName;
    private UUID planId;
    private String planName;
    private UUID saleId;
    private String contractNumber;
    private Contract.ContractStatus status;
    private BigDecimal monthlyFee;
    private Integer dueDay;
    private String installationAddress;
    private String city;
    private String state;
    private String zipCode;
    private LocalDateTime createdAt;
}
