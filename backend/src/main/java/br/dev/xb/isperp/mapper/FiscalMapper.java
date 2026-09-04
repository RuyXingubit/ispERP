package br.dev.xb.isperp.mapper;

import br.dev.xb.isperp.api.dto.*;
import br.dev.xb.isperp.entity.FiscalCompany;
import br.dev.xb.isperp.entity.FiscalGatewayConfig;
import br.dev.xb.isperp.entity.NfcomRecord;
import br.dev.xb.isperp.fiscal.FiscalGatewayType;
import br.dev.xb.isperp.fiscal.dto.CertificateUploadResult;
import br.dev.xb.isperp.fiscal.dto.NfcomCancelResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Mapper(componentModel = "spring")
public interface FiscalMapper {

    FiscalCompanyResponse toResponse(FiscalCompany entity);

    @Mapping(target = "hasCertificate", ignore = true)
    @Mapping(target = "certificateExpiresAt", ignore = true)
    @Mapping(target = "fiscalConfirmedAt", ignore = true)
    @Mapping(target = "accountingLastSentAt", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    FiscalCompany toEntity(FiscalCompanySaveRequest request);

    @Mapping(target = "gatewayType", expression = "java(config.getGatewayType() != null ? config.getGatewayType().name() : null)")
    FiscalGatewayConfigResponse toConfigResponse(FiscalGatewayConfig config);

    NfcomRecordResponse toNfcomRecordResponse(NfcomRecord record);

    List<NfcomRecordResponse> toNfcomRecordResponseList(List<NfcomRecord> records);

    default NfcomRecordsPageResponse toPageResponse(Page<NfcomRecord> page) {
        if (page == null) return null;
        NfcomRecordsPageResponse response = new NfcomRecordsPageResponse();
        response.setContent(toNfcomRecordResponseList(page.getContent()));
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setSize(page.getSize());
        response.setNumber(page.getNumber());
        return response;
    }

    CertificateUploadResultResponse toCertificateUploadResponse(CertificateUploadResult result);

    NfcomCancelResultResponse toCancelResponse(NfcomCancelResult result);

    default BigDecimal doubleToBigDecimal(Double value) {
        return value != null ? BigDecimal.valueOf(value) : null;
    }

    default Double bigDecimalToDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : null;
    }

    default FiscalRegime mapRegime(String regime) {
        if (regime == null) return null;
        try {
            return FiscalRegime.fromValue(regime);
        } catch (Exception e) {
            return null;
        }
    }

    default String mapRegimeString(FiscalRegime regime) {
        return regime != null ? regime.getValue() : null;
    }

    default FiscalEnvironment mapEnvironment(String env) {
        if (env == null) return null;
        try {
            return FiscalEnvironment.fromValue(env);
        } catch (Exception e) {
            return null;
        }
    }

    default String mapEnvironmentString(FiscalEnvironment env) {
        return env != null ? env.getValue() : null;
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
