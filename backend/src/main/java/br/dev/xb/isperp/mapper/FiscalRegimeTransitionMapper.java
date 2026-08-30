package br.dev.xb.isperp.mapper;

import br.dev.xb.isperp.dto.FiscalRegimeTransitionRequest;
import br.dev.xb.isperp.dto.FiscalRegimeTransitionResponse;
import br.dev.xb.isperp.entity.FiscalRegimeTransition;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FiscalRegimeTransitionMapper {

    FiscalRegimeTransitionResponse toDto(FiscalRegimeTransition entity);

    List<FiscalRegimeTransitionResponse> toDtoList(List<FiscalRegimeTransition> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "previousRegime", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "appliedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    FiscalRegimeTransition toEntity(FiscalRegimeTransitionRequest request);
}
