package br.dev.xb.isperp.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DisasterRecoveryMapper {

    @Mapping(target = "isDryRunVerified", expression = "java(dto.isDryRunVerified())")
    br.dev.xb.isperp.api.dto.BackupOverviewDto toOpenApiOverview(br.dev.xb.isperp.dto.backup.BackupOverviewDto dto);

    br.dev.xb.isperp.dto.backup.BackupPolicyRequest toDomainPolicyRequest(br.dev.xb.isperp.api.dto.BackupPolicyRequest request);

    @Mapping(target = "isActive", expression = "java(response.isActive())")
    br.dev.xb.isperp.api.dto.BackupPolicyResponse toOpenApiPolicyResponse(br.dev.xb.isperp.dto.backup.BackupPolicyResponse response);

    br.dev.xb.isperp.dto.backup.BackupDestinationRequest toDomainDestinationRequest(br.dev.xb.isperp.api.dto.BackupDestinationRequest request);

    @Mapping(target = "isActive", expression = "java(response.isActive())")
    @Mapping(target = "isPrimary", expression = "java(response.isPrimary())")
    br.dev.xb.isperp.api.dto.BackupDestinationResponse toOpenApiDestinationResponse(br.dev.xb.isperp.dto.backup.BackupDestinationResponse response);

    List<br.dev.xb.isperp.api.dto.BackupDestinationResponse> toOpenApiDestinationResponseList(List<br.dev.xb.isperp.dto.backup.BackupDestinationResponse> list);

    br.dev.xb.isperp.api.dto.StorageTestResult toOpenApiStorageTestResult(br.dev.xb.isperp.service.backup.StorageTestResult result);

    @Mapping(target = "isDryRunVerified", expression = "java(dto.isDryRunVerified())")
    br.dev.xb.isperp.api.dto.BackupExecutionLogDto toOpenApiExecutionLogDto(br.dev.xb.isperp.dto.backup.BackupExecutionLogDto dto);

    List<br.dev.xb.isperp.api.dto.BackupExecutionLogDto> toOpenApiExecutionLogDtoList(List<br.dev.xb.isperp.dto.backup.BackupExecutionLogDto> list);

    @Mapping(target = "isDryRunVerified", expression = "java(entity.getIsDryRunVerified())")
    @Mapping(target = "destinationName", ignore = true)
    br.dev.xb.isperp.api.dto.BackupExecutionLogDto toOpenApiExecutionLogDto(br.dev.xb.isperp.entity.backup.BackupExecutionLog entity);
}
