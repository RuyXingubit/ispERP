package br.dev.xb.isperp.mapper;

import br.dev.xb.isperp.dto.RadiusLifecycleLogResponse;
import br.dev.xb.isperp.dto.RadiusPolicyConfigRequest;
import br.dev.xb.isperp.dto.RadiusPolicyConfigResponse;
import br.dev.xb.isperp.entity.RadiusLifecycleLog;
import br.dev.xb.isperp.entity.RadiusPolicyConfig;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface RadiusLifecycleMapper {

    RadiusPolicyConfigResponse toConfigResponse(RadiusPolicyConfig config);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    RadiusPolicyConfig toConfigEntity(RadiusPolicyConfigRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateConfigEntityFromRequest(RadiusPolicyConfigRequest request, @MappingTarget RadiusPolicyConfig config);

    @Mapping(target = "customerName", ignore = true)
    RadiusLifecycleLogResponse toLogResponse(RadiusLifecycleLog log);
}
