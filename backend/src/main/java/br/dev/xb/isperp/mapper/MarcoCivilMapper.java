package br.dev.xb.isperp.mapper;

import br.dev.xb.isperp.dto.MarcoCivilReportRequest;
import br.dev.xb.isperp.dto.MarcoCivilReportResponse;
import br.dev.xb.isperp.entity.MarcoCivilReport;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface MarcoCivilMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "validationToken", ignore = true)
    @Mapping(target = "sha256Hash", ignore = true)
    @Mapping(target = "matchedCustomerName", ignore = true)
    @Mapping(target = "matchedCpfCnpj", ignore = true)
    @Mapping(target = "matchedCallingStationId", ignore = true)
    @Mapping(target = "matchedSessionStart", ignore = true)
    @Mapping(target = "matchedSessionStop", ignore = true)
    @Mapping(target = "reportPdfUrl", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    MarcoCivilReport toEntity(MarcoCivilReportRequest request);

    @Mapping(target = "publicValidationUrl", ignore = true)
    @Mapping(target = "qrCodePayload", ignore = true)
    MarcoCivilReportResponse toResponse(MarcoCivilReport entity);
}
