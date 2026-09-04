package br.dev.xb.isperp.mapper;

import br.dev.xb.isperp.api.dto.OnuResponse;
import br.dev.xb.isperp.api.dto.OnuStatusResponse;
import br.dev.xb.isperp.entity.OnuProvisioning;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface OnuMapper {

    OnuResponse toResponse(OnuProvisioning provisioning);

    List<OnuResponse> toResponseList(List<OnuProvisioning> provisionings);

    OnuStatusResponse toApiStatusResponse(br.dev.xb.isperp.network.dto.OnuStatusResponse status);

    default Double bigDecimalToDouble(BigDecimal val) {
        if (val == null) return null;
        return val.doubleValue();
    }

    default BigDecimal doubleToBigDecimal(Double val) {
        if (val == null) return null;
        return BigDecimal.valueOf(val);
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
