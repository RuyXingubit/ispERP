package br.dev.xb.isperp.dto.financial;

import br.dev.xb.isperp.entity.financial.FeeStatus;
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
public class WorkOrderFeeDto {
    private UUID workOrderId;
    private String protocol;
    private UUID customerId;
    private String customerName;
    private String serviceType;
    private BigDecimal standardFeeAmount;
    private FeeStatus feeStatus;
    private String waiverReason;
    private UUID waiverRequestedByUserId;
    private String waiverRequestedByName;
    private UUID waiverAuditedByUserId;
    private String waiverAuditedByName;
    private OffsetDateTime waiverAuditedAt;
}
