package br.dev.xb.isperp.mapper;

import br.dev.xb.isperp.api.dto.WorkOrderResponse;
import br.dev.xb.isperp.dto.WorkOrderDTO;
import br.dev.xb.isperp.entity.WorkOrder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ValueMapping;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

@Mapper(componentModel = "spring")
public interface WorkOrderMapper {

    @Mapping(target = "contractNumber", ignore = true)
    @Mapping(target = "customerId", ignore = true)
    @Mapping(target = "customerName", ignore = true)
    @Mapping(target = "customerPhone", ignore = true)
    @Mapping(target = "installationAddress", ignore = true)
    WorkOrderDTO toDto(WorkOrder workOrder);

    @Mapping(target = "technicianLatitude", ignore = true)
    @Mapping(target = "technicianLongitude", ignore = true)
    @Mapping(target = "gpsCapturedAt", ignore = true)
    @Mapping(target = "digitalSignatureBase64", ignore = true)
    @Mapping(target = "customerSignatureName", ignore = true)
    @Mapping(target = "installationPhotoUrl", ignore = true)
    @Mapping(target = "toolAgreementId", ignore = true)
    @Mapping(target = "onuRxPowerDbm", ignore = true)
    @Mapping(target = "radiusAuthenticated", ignore = true)
    @Mapping(target = "allocatedWarehouseId", ignore = true)
    @Mapping(target = "ctoId", ignore = true)
    @Mapping(target = "ctoPortNumber", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "standardFeeAmount", ignore = true)
    @Mapping(target = "feeStatus", ignore = true)
    @Mapping(target = "waiverReason", ignore = true)
    @Mapping(target = "waiverRequestedByUserId", ignore = true)
    @Mapping(target = "waiverAuditedByUserId", ignore = true)
    @Mapping(target = "waiverAuditedAt", ignore = true)
    WorkOrder toEntity(WorkOrderDTO dto);

    List<WorkOrderDTO> toDtoList(List<WorkOrder> workOrders);

    // Mapeamentos para Contratos OpenAPI (API-First)
    WorkOrderResponse toResponse(WorkOrder workOrder);

    List<WorkOrderResponse> toResponseList(List<WorkOrder> workOrders);

    br.dev.xb.isperp.dto.ScheduleWorkOrderRequest toScheduleRequest(br.dev.xb.isperp.api.dto.ScheduleWorkOrderRequest request);

    br.dev.xb.isperp.dto.CompleteWorkOrderRequest toCompleteRequest(br.dev.xb.isperp.api.dto.CompleteWorkOrderRequest request);

    @ValueMapping(source = "CANCELED", target = "CANCELLED")
    br.dev.xb.isperp.api.dto.WorkOrderStatus toDtoStatus(WorkOrder.WorkOrderStatus status);

    @ValueMapping(source = "CANCELLED", target = "CANCELED")
    @ValueMapping(source = "BLOCKED", target = "PENDING_SCHEDULE")
    WorkOrder.WorkOrderStatus toEntityStatus(br.dev.xb.isperp.api.dto.WorkOrderStatus status);

    default BigDecimal doubleToBigDecimal(Double value) {
        return value != null ? BigDecimal.valueOf(value) : null;
    }

    default Double bigDecimalToDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : null;
    }

    default URI stringToUri(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return URI.create(value);
        } catch (Exception e) {
            return null;
        }
    }

    default String uriToString(URI value) {
        return value != null ? value.toString() : null;
    }
}
