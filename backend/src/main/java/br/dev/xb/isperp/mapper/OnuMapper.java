package br.dev.xb.isperp.mapper;

import br.dev.xb.isperp.api.dto.OnuResponse;
import br.dev.xb.isperp.api.dto.OnuStatusResponse;
import br.dev.xb.isperp.entity.OnuProvisioning;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
@SuppressWarnings("java:S2583")
public interface OnuMapper extends CommonMapperConverters {

    OnuResponse toResponse(OnuProvisioning provisioning);

    List<OnuResponse> toResponseList(List<OnuProvisioning> provisionings);

    OnuStatusResponse toApiStatusResponse(br.dev.xb.isperp.network.dto.OnuStatusResponse status);
}
