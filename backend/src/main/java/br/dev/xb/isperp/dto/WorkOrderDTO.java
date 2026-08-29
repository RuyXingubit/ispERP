package br.dev.xb.isperp.dto;

import br.dev.xb.isperp.entity.WorkOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderDTO {

    private UUID id;
    private UUID contractId;
    private String contractNumber;
    private UUID customerId;
    private String customerName;
    private String customerPhone;
    private String installationAddress;
    private WorkOrder.WorkOrderType type;
    private WorkOrder.WorkOrderStatus status;
    private LocalDate scheduledDate;
    private String scheduledPeriod;
    private String technicianName;
    private String onuMac;
    private String onuSerial;
    private BigDecimal fiberSignalDbm;
    private String notes;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}
