package br.dev.xb.isperp.mapper;

import br.dev.xb.isperp.dto.FiscalRegimeTransitionRequest;
import br.dev.xb.isperp.dto.FiscalRegimeTransitionResponse;
import br.dev.xb.isperp.entity.FiscalRegimeTransition;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
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

    br.dev.xb.isperp.api.dto.FiscalRegimeTransitionResponse toApiResponse(FiscalRegimeTransitionResponse dto);

    List<br.dev.xb.isperp.api.dto.FiscalRegimeTransitionResponse> toApiResponseList(List<FiscalRegimeTransitionResponse> dtos);

    FiscalRegimeTransitionRequest toInternalRequest(br.dev.xb.isperp.api.dto.FiscalRegimeTransitionRequest request);

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

    default br.dev.xb.isperp.api.dto.FiscalRegimeTransitionStatus mapStatus(br.dev.xb.isperp.fiscal.FiscalRegimeTransitionStatus status) {
        if (status == null) return null;
        return br.dev.xb.isperp.api.dto.FiscalRegimeTransitionStatus.fromValue(status.name());
    }

    default br.dev.xb.isperp.fiscal.FiscalRegimeTransitionStatus mapStatus(br.dev.xb.isperp.api.dto.FiscalRegimeTransitionStatus status) {
        if (status == null) return null;
        return br.dev.xb.isperp.fiscal.FiscalRegimeTransitionStatus.valueOf(status.getValue());
    }
}
