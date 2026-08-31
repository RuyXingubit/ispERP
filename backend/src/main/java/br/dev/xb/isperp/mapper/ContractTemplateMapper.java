package br.dev.xb.isperp.mapper;

import br.dev.xb.isperp.dto.ContractTemplateRequest;
import br.dev.xb.isperp.dto.ContractTemplateResponse;
import br.dev.xb.isperp.entity.ContractTemplate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ContractTemplateMapper {

    ContractTemplateResponse toResponse(ContractTemplate entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ContractTemplate toEntity(ContractTemplateRequest request);

    List<ContractTemplateResponse> toResponseList(List<ContractTemplate> entities);
}
