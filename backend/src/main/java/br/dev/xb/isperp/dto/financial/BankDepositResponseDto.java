package br.dev.xb.isperp.dto.financial;

import br.dev.xb.isperp.entity.financial.BankDepositStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankDepositResponseDto {
    private UUID id;
    private UUID depositorUserId;
    private String depositorUserName;
    private String depositorCpf;
    private BigDecimal amount;
    private String bankName;
    private String bankAgency;
    private String bankAccount;
    private String receiptFileUrl;
    private OffsetDateTime depositDate;
    private BankDepositStatus status;
    private UUID auditedByUserId;
    private String auditedByUserName;
    private OffsetDateTime auditedAt;
    private String notes;
    private String rejectionReason;
}
