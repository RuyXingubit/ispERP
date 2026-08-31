package br.dev.xb.isperp.mapper;

import br.dev.xb.isperp.dto.WorkOrderDTO;
import br.dev.xb.isperp.entity.WorkOrder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

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
    WorkOrder toEntity(WorkOrderDTO dto);

    List<WorkOrderDTO> toDtoList(List<WorkOrder> workOrders);
}
