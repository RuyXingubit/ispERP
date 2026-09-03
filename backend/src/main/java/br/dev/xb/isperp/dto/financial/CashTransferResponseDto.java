package br.dev.xb.isperp.dto.financial;

import br.dev.xb.isperp.entity.financial.CashTransferStatus;
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
public class CashTransferResponseDto {
    private UUID id;
    private UUID senderUserId;
    private String senderUserName;
    private UUID receiverUserId;
    private String receiverUserName;
    private BigDecimal amount;
    private String reason;
    private CashTransferStatus status;
    private OffsetDateTime requestedAt;
    private OffsetDateTime respondedAt;
    private String notes;
}
