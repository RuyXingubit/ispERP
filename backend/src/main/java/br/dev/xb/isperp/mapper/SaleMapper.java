package br.dev.xb.isperp.mapper;

import br.dev.xb.isperp.api.dto.CreateSaleRequest;
import br.dev.xb.isperp.api.dto.SaleResponse;
import br.dev.xb.isperp.api.dto.SaleStatusEnum;
import br.dev.xb.isperp.entity.Sale;
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
public interface SaleMapper {

    @Mapping(target = "status", expression = "java(toDtoStatus(sale.getStatus()))")
    SaleResponse toResponse(Sale sale);

    List<SaleResponse> toResponseList(List<Sale> sales);

    br.dev.xb.isperp.dto.CreateSaleRequest toLegacyRequest(CreateSaleRequest request);

    default SaleStatusEnum toDtoStatus(Sale.SaleStatus status) {
        if (status == null) return null;
        return SaleStatusEnum.valueOf(status.name());
    }

    default Sale.SaleStatus toEntityStatus(SaleStatusEnum status) {
        if (status == null) return null;
        return Sale.SaleStatus.valueOf(status.getValue());
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
