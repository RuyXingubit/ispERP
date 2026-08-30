package br.dev.xb.isperp.mapper;

import br.dev.xb.isperp.dto.NasRequest;
import br.dev.xb.isperp.dto.NasResponse;
import br.dev.xb.isperp.dto.RadiusSessionResponse;
import br.dev.xb.isperp.entity.Nas;
import br.dev.xb.isperp.entity.RadAcct;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface RadiusMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Nas toEntity(NasRequest request);

    NasResponse toResponse(Nas entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(NasRequest request, @MappingTarget Nas entity);

    @Mapping(target = "isOnline", expression = "java(entity.getAcctStopTime() == null)")
    @Mapping(target = "nasShortname", ignore = true)
    @Mapping(target = "customerName", ignore = true)
    @Mapping(target = "customerCpfCnpj", ignore = true)
    RadiusSessionResponse toSessionResponse(RadAcct entity);
}
