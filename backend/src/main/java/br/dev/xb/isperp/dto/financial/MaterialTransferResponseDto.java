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
public class MaterialTransferResponseDto {
    private UUID id;
    private UUID senderUserId;
    private String senderUserName;
    private UUID receiverUserId;
    private String receiverUserName;
    private UUID materialCustodyId;
    private String itemName;
    private String serialNumber;
    private BigDecimal quantity;
    private CashTransferStatus status;
    private OffsetDateTime requestedAt;
    private OffsetDateTime respondedAt;
    private String notes;
}
