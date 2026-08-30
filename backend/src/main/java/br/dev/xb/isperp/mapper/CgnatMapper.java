package br.dev.xb.isperp.mapper;

import br.dev.xb.isperp.dto.CgnatMappingRequest;
import br.dev.xb.isperp.dto.CgnatMappingResponse;
import br.dev.xb.isperp.entity.CgnatMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CgnatMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "nas", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    CgnatMapping toEntity(CgnatMappingRequest request);

    @Mapping(target = "nasId", source = "nas.id")
    @Mapping(target = "nasName", source = "nas.nasname")
    CgnatMappingResponse toResponse(CgnatMapping entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "nas", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(CgnatMappingRequest request, @MappingTarget CgnatMapping entity);
}
