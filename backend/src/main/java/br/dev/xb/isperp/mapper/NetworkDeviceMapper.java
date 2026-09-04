package br.dev.xb.isperp.mapper;

import br.dev.xb.isperp.api.dto.NetworkDeviceCreateRequest;
import br.dev.xb.isperp.api.dto.NetworkDeviceResponse;
import br.dev.xb.isperp.entity.NetworkDevice;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface NetworkDeviceMapper {

    NetworkDeviceResponse toResponse(NetworkDevice device);

    List<NetworkDeviceResponse> toResponseList(List<NetworkDevice> devices);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    NetworkDevice toEntity(NetworkDeviceCreateRequest request);

    default OffsetDateTime localDateTimeToOffsetDateTime(LocalDateTime localDateTime) {
        if (localDateTime == null) return null;
        return localDateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }

    default LocalDateTime offsetDateTimeToLocalDateTime(OffsetDateTime offsetDateTime) {
        if (offsetDateTime == null) return null;
        return offsetDateTime.toLocalDateTime();
    }
}
