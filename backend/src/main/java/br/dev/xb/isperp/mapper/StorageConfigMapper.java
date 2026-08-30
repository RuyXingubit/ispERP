package br.dev.xb.isperp.mapper;

import br.dev.xb.isperp.dto.StorageConfigRequest;
import br.dev.xb.isperp.dto.StorageConfigResponse;
import br.dev.xb.isperp.entity.StorageConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface StorageConfigMapper {

    @Mapping(target = "maskedSecretKey", source = "secretKey", qualifiedByName = "maskSecret")
    StorageConfigResponse toDto(StorageConfig entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    StorageConfig toEntity(StorageConfigRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(StorageConfigRequest request, @MappingTarget StorageConfig entity);

    @Named("maskSecret")
    default String maskSecret(String secretKey) {
        if (secretKey == null || secretKey.isBlank()) {
            return "";
        }
        if (secretKey.length() <= 4) {
            return "••••";
        }
        return "••••••••" + secretKey.substring(secretKey.length() - 4);
    }
}
