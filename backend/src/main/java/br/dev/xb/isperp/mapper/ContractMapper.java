package br.dev.xb.isperp.mapper;

import br.dev.xb.isperp.dto.ContractDTO;
import br.dev.xb.isperp.entity.Contract;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ContractMapper {

    @Mapping(target = "customerName", ignore = true)
    @Mapping(target = "planName", ignore = true)
    ContractDTO toDto(Contract contract);

    @Mapping(target = "customSuspensionDays", ignore = true)
    @Mapping(target = "ctoId", ignore = true)
    @Mapping(target = "ctoPortNumber", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "pendingOnboardingCredit", ignore = true)
    Contract toEntity(ContractDTO dto);

    List<ContractDTO> toDtoList(List<Contract> contracts);
}
