package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.StorageConfigRequest;
import br.dev.xb.isperp.dto.StorageConfigResponse;
import br.dev.xb.isperp.dto.StorageConnectionTestResponse;
import br.dev.xb.isperp.entity.StorageConfig;
import br.dev.xb.isperp.mapper.StorageConfigMapper;
import br.dev.xb.isperp.repository.StorageConfigRepository;
import br.dev.xb.isperp.storage.StorageProvider;
import br.dev.xb.isperp.storage.StorageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

import java.net.URI;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class StorageConfigService {

    private final StorageConfigRepository storageConfigRepository;
    private final StorageConfigMapper storageConfigMapper;

    @Value("${app.storage.type:s3}")
    private String defaultStorageType;

    @Value("${app.storage.s3.endpoint:http://localhost:8333}")
    private String defaultEndpointUrl;

    @Value("${app.storage.s3.bucket:isperp-files}")
    private String defaultBucketName;

    @Value("${app.storage.s3.region:us-east-1}")
    private String defaultRegion;

    @Value("${app.storage.s3.access-key:}")
    private String defaultAccessKey;

    @Value("${app.storage.s3.secret-key:}")
    private String defaultSecretKey;

    @Value("${app.storage.s3.path-style-access:true}")
    private boolean defaultPathStyleAccess;

    @Transactional(readOnly = true)
    public StorageConfigResponse getActiveConfig() {
        Optional<StorageConfig> configOpt = storageConfigRepository.findFirstByIsActiveTrueOrderByCreatedAtDesc();
        if (configOpt.isPresent()) {
            return storageConfigMapper.toDto(configOpt.get());
        }

        // Retorna configuração padrão baseada nas propriedades de ambiente
        return StorageConfigResponse.builder()
                .storageType("local".equalsIgnoreCase(defaultStorageType) ? StorageType.LOCAL : StorageType.S3)
                .provider(StorageProvider.SEAWEEDFS_LOCAL)
                .endpointUrl(defaultEndpointUrl)
                .bucketName(defaultBucketName)
                .region(defaultRegion)
                .accessKey(defaultAccessKey)
                .maskedSecretKey(storageConfigMapper.maskSecret(defaultSecretKey))
                .pathStyleAccess(defaultPathStyleAccess)
                .isActive(true)
                .build();
    }

    @Transactional
    public StorageConfigResponse saveOrUpdate(StorageConfigRequest request) {
        Optional<StorageConfig> existingOpt = storageConfigRepository.findFirstByIsActiveTrueOrderByCreatedAtDesc();

        StorageConfig config;
        if (existingOpt.isPresent()) {
            config = existingOpt.get();
            String existingSecret = config.getSecretKey();
            storageConfigMapper.updateEntityFromRequest(request, config);

            // Preserva secretKey anterior se não foi alterada na requisição
            if ((request.getSecretKey() == null || request.getSecretKey().isBlank()) && existingSecret != null) {
                config.setSecretKey(existingSecret);
            }
        } else {
            config = storageConfigMapper.toEntity(request);
        }

        StorageConfig saved = storageConfigRepository.save(config);
        log.info("⚙️ [StorageConfig] Configuração de armazenamento salva com sucesso. Provedor: {}, Bucket: {}",
                saved.getProvider(), saved.getBucketName());
        return storageConfigMapper.toDto(saved);
    }

    public StorageConnectionTestResponse testConnection(StorageConfigRequest request) {
        long startTime = System.currentTimeMillis();

        if (request.getStorageType() == StorageType.LOCAL) {
            long latency = System.currentTimeMillis() - startTime;
            return StorageConnectionTestResponse.builder()
                    .success(true)
                    .message("Armazenamento em disco local ativo e verificado.")
                    .details("Diretório local configurado com sucesso.")
                    .latencyMs(latency)
                    .build();
        }

        // Caso secretKey venha em branco no teste, verifica se há chave salva no banco
        String effectiveSecretKey = request.getSecretKey();
        if (effectiveSecretKey == null || effectiveSecretKey.isBlank()) {
            Optional<StorageConfig> existing = storageConfigRepository.findFirstByIsActiveTrueOrderByCreatedAtDesc();
            if (existing.isPresent()) {
                effectiveSecretKey = existing.get().getSecretKey();
            } else {
                effectiveSecretKey = defaultSecretKey;
            }
        }

        String effectiveAccessKey = request.getAccessKey() != null && !request.getAccessKey().isBlank()
                ? request.getAccessKey() : defaultAccessKey;
        String effectiveEndpoint = request.getEndpointUrl() != null && !request.getEndpointUrl().isBlank()
                ? request.getEndpointUrl() : defaultEndpointUrl;
        String effectiveBucket = request.getBucketName() != null && !request.getBucketName().isBlank()
                ? request.getBucketName() : defaultBucketName;
        String effectiveRegion = request.getRegion() != null && !request.getRegion().isBlank()
                ? request.getRegion() : defaultRegion;
        boolean effectivePathStyle = request.getPathStyleAccess() != null
                ? request.getPathStyleAccess() : defaultPathStyleAccess;

        try (S3Client s3Client = buildS3Client(effectiveEndpoint, effectiveRegion, effectiveAccessKey,
                effectiveSecretKey, effectivePathStyle)) {

            try {
                s3Client.headBucket(HeadBucketRequest.builder().bucket(effectiveBucket).build());
            } catch (NoSuchBucketException e) {
                // Tenta criar o bucket se não existir
                log.info("Bucket {} não existe no endpoint {}. Tentando criar...", effectiveBucket, effectiveEndpoint);
                s3Client.createBucket(CreateBucketRequest.builder().bucket(effectiveBucket).build());
            }

            long latency = System.currentTimeMillis() - startTime;
            log.info("✅ [StorageTest] Conexão S3 com {} (bucket: {}) realizada com sucesso em {} ms",
                    effectiveEndpoint, effectiveBucket, latency);

            return StorageConnectionTestResponse.builder()
                    .success(true)
                    .message("Conexão S3 estabelecida com sucesso!")
                    .details("Bucket '" + effectiveBucket + "' conectado e operacional (" + request.getProvider() + ").")
                    .latencyMs(latency)
                    .build();

        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            log.warn("❌ [StorageTest] Falha no teste de conexão S3 com {}: {}", effectiveEndpoint, e.getMessage());
            return StorageConnectionTestResponse.builder()
                    .success(false)
                    .message("Falha ao conectar no Storage S3: " + e.getMessage())
                    .details(e.getClass().getSimpleName())
                    .latencyMs(latency)
                    .build();
        }
    }

    public S3Client buildS3Client(String endpointUrl, String region, String accessKey, String secretKey, boolean pathStyleAccess) {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region != null && !region.isBlank() ? region : "us-east-1"));

        if (endpointUrl != null && !endpointUrl.isBlank()) {
            builder.endpointOverride(URI.create(endpointUrl));
        }

        builder.serviceConfiguration(S3Configuration.builder()
                .pathStyleAccessEnabled(pathStyleAccess)
                .build());

        if (accessKey != null && !accessKey.isBlank() && secretKey != null && !secretKey.isBlank()) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey)
            ));
        } else {
            builder.credentialsProvider(AnonymousCredentialsProvider.create());
        }

        return builder.build();
    }
}
