package br.dev.xb.isperp.mapper;

import br.dev.xb.isperp.api.dto.ContractCreateRequest;
import br.dev.xb.isperp.api.dto.ContractResponse;
import br.dev.xb.isperp.api.dto.ContractUpdateRequest;
import br.dev.xb.isperp.dto.ContractDTO;
import br.dev.xb.isperp.entity.Contract;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ContractMapper {

    ContractResponse toResponse(Contract contract);

    List<ContractResponse> toResponseList(List<Contract> contracts);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Contract toEntity(ContractCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customerId", ignore = true)
    @Mapping(target = "saleId", ignore = true)
    @Mapping(target = "contractNumber", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(ContractUpdateRequest request, @MappingTarget Contract contract);

    // Compatibilidade legado com ContractDTO
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

    @ValueMapping(source = "CANCELED", target = "CANCELLED")
    br.dev.xb.isperp.api.dto.ContractStatus toDtoStatus(Contract.ContractStatus status);

    @ValueMapping(source = "CANCELLED", target = "CANCELED")
    @ValueMapping(source = "PENDING_SIGNATURE", target = "DRAFT")
    Contract.ContractStatus toEntityStatus(br.dev.xb.isperp.api.dto.ContractStatus status);

    @AfterMapping
    default void setDefaults(@MappingTarget Contract contract) {
        if (contract.getPendingOnboardingCredit() == null) {
            contract.setPendingOnboardingCredit(BigDecimal.ZERO);
        }
    }

    default BigDecimal doubleToBigDecimal(Double value) {
        return value != null ? BigDecimal.valueOf(value) : null;
    }

    default Double bigDecimalToDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : null;
    }

    default OffsetDateTime localDateTimeToOffsetDateTime(LocalDateTime localDateTime) {
        if (localDateTime == null) return null;
        return localDateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }

    default LocalDateTime offsetDateTimeToLocalDateTime(OffsetDateTime offsetDateTime) {
        if (offsetDateTime == null) return null;
        return offsetDateTime.toLocalDateTime();
    }
}
