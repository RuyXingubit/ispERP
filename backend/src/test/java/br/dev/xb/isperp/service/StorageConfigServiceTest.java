package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.StorageConfigRequest;
import br.dev.xb.isperp.dto.StorageConfigResponse;
import br.dev.xb.isperp.dto.StorageConnectionTestResponse;
import br.dev.xb.isperp.entity.StorageConfig;
import br.dev.xb.isperp.mapper.StorageConfigMapper;
import org.mapstruct.factory.Mappers;
import br.dev.xb.isperp.repository.StorageConfigRepository;
import br.dev.xb.isperp.storage.StorageProvider;
import br.dev.xb.isperp.storage.StorageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StorageConfigServiceTest {

    @Mock
    private StorageConfigRepository storageConfigRepository;

    private StorageConfigMapper storageConfigMapper = Mappers.getMapper(StorageConfigMapper.class);

    private StorageConfigService storageConfigService;

    @BeforeEach
    void setUp() {
        storageConfigService = new StorageConfigService(storageConfigRepository, storageConfigMapper);
        ReflectionTestUtils.setField(storageConfigService, "defaultStorageType", "s3");
        ReflectionTestUtils.setField(storageConfigService, "defaultEndpointUrl", "http://localhost:8333");
        ReflectionTestUtils.setField(storageConfigService, "defaultBucketName", "isperp-files");
        ReflectionTestUtils.setField(storageConfigService, "defaultRegion", "us-east-1");
        ReflectionTestUtils.setField(storageConfigService, "defaultAccessKey", "testAccess");
        ReflectionTestUtils.setField(storageConfigService, "defaultSecretKey", "testSecret1234");
        ReflectionTestUtils.setField(storageConfigService, "defaultPathStyleAccess", true);
    }

    @Test
    @DisplayName("Deve retornar configuração padrão do ambiente quando não houver registro no banco")
    void shouldReturnDefaultConfigWhenNoRecordInDatabase() {
        when(storageConfigRepository.findFirstByIsActiveTrueOrderByCreatedAtDesc()).thenReturn(Optional.empty());

        StorageConfigResponse response = storageConfigService.getActiveConfig();

        assertThat(response).isNotNull();
        assertThat(response.getProvider()).isEqualTo(StorageProvider.SEAWEEDFS_LOCAL);
        assertThat(response.getBucketName()).isEqualTo("isperp-files");
        assertThat(response.getEndpointUrl()).isEqualTo("http://localhost:8333");
        assertThat(response.getMaskedSecretKey()).contains("••••");
    }

    @Test
    @DisplayName("Deve salvar ou atualizar configuração de storage no banco")
    void shouldSaveOrUpdateStorageConfig() {
        StorageConfigRequest request = StorageConfigRequest.builder()
                .storageType(StorageType.S3)
                .provider(StorageProvider.AWS_S3)
                .endpointUrl("https://s3.us-east-1.amazonaws.com")
                .bucketName("meu-bucket-isp")
                .region("sa-east-1")
                .accessKey("AKIA1234567890")
                .secretKey("MinhaSecretSuperSegura999")
                .pathStyleAccess(false)
                .isActive(true)
                .build();

        StorageConfig savedEntity = StorageConfig.builder()
                .id(UUID.randomUUID())
                .storageType(StorageType.S3)
                .provider(StorageProvider.AWS_S3)
                .endpointUrl("https://s3.us-east-1.amazonaws.com")
                .bucketName("meu-bucket-isp")
                .region("sa-east-1")
                .accessKey("AKIA1234567890")
                .secretKey("MinhaSecretSuperSegura999")
                .pathStyleAccess(false)
                .isActive(true)
                .build();

        when(storageConfigRepository.findFirstByIsActiveTrueOrderByCreatedAtDesc()).thenReturn(Optional.empty());
        when(storageConfigRepository.save(any(StorageConfig.class))).thenReturn(savedEntity);

        StorageConfigResponse response = storageConfigService.saveOrUpdate(request);

        assertThat(response).isNotNull();
        assertThat(response.getProvider()).isEqualTo(StorageProvider.AWS_S3);
        assertThat(response.getBucketName()).isEqualTo("meu-bucket-isp");
        assertThat(response.getMaskedSecretKey()).isEqualTo("••••••••a999");
        verify(storageConfigRepository, times(1)).save(any(StorageConfig.class));
    }

    @Test
    @DisplayName("Deve validar teste de conexão imediato para Local Storage")
    void shouldValidateConnectionForLocalStorage() {
        StorageConfigRequest request = StorageConfigRequest.builder()
                .storageType(StorageType.LOCAL)
                .provider(StorageProvider.LOCAL_DISK)
                .bucketName("isperp-files")
                .build();

        StorageConnectionTestResponse testResponse = storageConfigService.testConnection(request);

        assertThat(testResponse.isSuccess()).isTrue();
        assertThat(testResponse.getMessage()).contains("Armazenamento em disco local");
    }
}
