package br.dev.xb.isperp.mapper;

import br.dev.xb.isperp.dto.PlanDTO;
import br.dev.xb.isperp.entity.Plan;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PlanMapper {

    PlanDTO toDto(Plan plan);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "suspensionDays", ignore = true)
    @Mapping(target = "alwaysIssueNfcom", ignore = true)
    Plan toEntity(PlanDTO dto);

    List<PlanDTO> toDtoList(List<Plan> plans);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "suspensionDays", ignore = true)
    @Mapping(target = "alwaysIssueNfcom", ignore = true)
    void updateEntityFromDto(PlanDTO dto, @MappingTarget Plan entity);
}
