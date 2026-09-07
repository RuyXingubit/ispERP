package br.dev.xb.isperp.mapper;

import br.dev.xb.isperp.api.dto.DashboardBiResponse;
import br.dev.xb.isperp.dto.DashboardBiDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DashboardBiMapper {
    DashboardBiResponse toOpenApiResponse(DashboardBiDTO dto);
}
