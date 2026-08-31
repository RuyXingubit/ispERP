package br.dev.xb.isperp.mapper;

import br.dev.xb.isperp.dto.InstallationMaterialDemandResponse;
import br.dev.xb.isperp.entity.InstallationMaterialDemand;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface InstallationMaterialDemandMapper {

    @Mapping(target = "contractNumber", ignore = true)
    @Mapping(target = "customerName", ignore = true)
    @Mapping(target = "customerPhone", ignore = true)
    @Mapping(target = "customerAddress", ignore = true)
    @Mapping(target = "customerLatitude", ignore = true)
    @Mapping(target = "customerLongitude", ignore = true)
    @Mapping(target = "ctoName", ignore = true)
    @Mapping(target = "ctoLatitude", ignore = true)
    @Mapping(target = "ctoLongitude", ignore = true)
    @Mapping(target = "allocatedWarehouseName", ignore = true)
    InstallationMaterialDemandResponse toResponse(InstallationMaterialDemand entity);

    List<InstallationMaterialDemandResponse> toResponseList(List<InstallationMaterialDemand> entities);
}
