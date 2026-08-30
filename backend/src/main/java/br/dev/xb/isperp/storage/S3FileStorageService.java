package br.dev.xb.isperp.storage;

import br.dev.xb.isperp.entity.StorageConfig;
import br.dev.xb.isperp.exception.BusinessException;
import br.dev.xb.isperp.repository.StorageConfigRepository;
import br.dev.xb.isperp.service.StorageConfigService;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Optional;

@Service
@Primary
@Slf4j
@SuppressWarnings("null")
public class S3FileStorageService implements FileStorageService {

    private final LocalFileStorageService localFileStorageService;
    private final StorageConfigRepository storageConfigRepository;
    private final StorageConfigService storageConfigService;

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

    private volatile boolean bucketChecked = false;

    public S3FileStorageService(LocalFileStorageService localFileStorageService,
                                StorageConfigRepository storageConfigRepository,
                                StorageConfigService storageConfigService) {
        this.localFileStorageService = localFileStorageService;
        this.storageConfigRepository = storageConfigRepository;
        this.storageConfigService = storageConfigService;
    }

    @Override
    public String store(String originalFilename, String contentType, InputStream inputStream) {
        StorageConfigResolved config = resolveConfig();

        if (config.storageType() == StorageType.LOCAL) {
            return localFileStorageService.store(originalFilename, contentType, inputStream);
        }

        try {
            String extension = "";
            int dotIndex = originalFilename.lastIndexOf('.');
            if (dotIndex >= 0) {
                extension = originalFilename.substring(dotIndex);
            }
            String uniqueKey = UuidCreatorUtils.generateUuidV7().toString() + extension;

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[8192];
            int nRead;
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            byte[] fileBytes = buffer.toByteArray();

            try (S3Client s3Client = buildClient(config)) {
                ensureBucket(s3Client, config.bucketName());

                PutObjectRequest putRequest = PutObjectRequest.builder()
                        .bucket(config.bucketName())
                        .key(uniqueKey)
                        .contentType(contentType != null ? contentType : "application/octet-stream")
                        .contentLength((long) fileBytes.length)
                        .build();

                s3Client.putObject(putRequest, RequestBody.fromBytes(fileBytes));
                log.info("☁️ [S3Storage] Arquivo armazenado com sucesso no bucket '{}': {}", config.bucketName(), uniqueKey);
            }

            return uniqueKey;
        } catch (Exception e) {
            log.error("Erro ao armazenar arquivo no S3: {}", e.getMessage(), e);
            throw new BusinessException("Não foi possível armazenar o arquivo no S3: " + e.getMessage(), e);
        }
    }

    @Override
    public Resource recover(String storedFilename) {
        StorageConfigResolved config = resolveConfig();

        if (config.storageType() == StorageType.LOCAL) {
            return localFileStorageService.recover(storedFilename);
        }

        try {
            S3Client s3Client = buildClient(config);
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(config.bucketName())
                    .key(storedFilename)
                    .build();

            ResponseInputStream<GetObjectResponse> responseStream = s3Client.getObject(getRequest);
            return new InputStreamResource(responseStream) {
                @Override
                public String getFilename() {
                    return storedFilename;
                }

                @Override
                public long contentLength() {
                    return responseStream.response().contentLength() != null
                            ? responseStream.response().contentLength() : -1;
                }
            };
        } catch (NoSuchKeyException e) {
            log.warn("Arquivo não encontrado no S3: {}", storedFilename);
            throw new BusinessException("Arquivo não encontrado no storage: " + storedFilename, e);
        } catch (Exception e) {
            log.error("Erro ao recuperar arquivo do S3: {}", e.getMessage(), e);
            throw new BusinessException("Erro ao recuperar arquivo do S3: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String storedFilename) {
        StorageConfigResolved config = resolveConfig();

        if (config.storageType() == StorageType.LOCAL) {
            localFileStorageService.delete(storedFilename);
            return;
        }

        try (S3Client s3Client = buildClient(config)) {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(config.bucketName())
                    .key(storedFilename)
                    .build();

            s3Client.deleteObject(deleteRequest);
            log.info("🗑️ [S3Storage] Arquivo removido com sucesso do bucket '{}': {}", config.bucketName(), storedFilename);
        } catch (Exception e) {
            log.warn("Falha ao remover arquivo {} do S3: {}", storedFilename, e.getMessage());
        }
    }

    @Override
    public String getFileUrl(String storedFilename) {
        return "/api/files/" + storedFilename;
    }

    private void ensureBucket(S3Client s3Client, String bucketName) {
        if (!bucketChecked) {
            synchronized (this) {
                if (!bucketChecked) {
                    try {
                        s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
                        bucketChecked = true;
                    } catch (NoSuchBucketException e) {
                        try {
                            log.info("Criando bucket S3 inicial '{}'...", bucketName);
                            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
                            bucketChecked = true;
                        } catch (Exception createEx) {
                            log.warn("Não foi possível criar o bucket automaticamete: {}", createEx.getMessage());
                        }
                    } catch (Exception e) {
                        log.debug("Checagem de bucket headBucket retornou: {}", e.getMessage());
                    }
                }
            }
        }
    }

    private S3Client buildClient(StorageConfigResolved config) {
        return storageConfigService.buildS3Client(
                config.endpointUrl(),
                config.region(),
                config.accessKey(),
                config.secretKey(),
                config.pathStyleAccess()
        );
    }

    private StorageConfigResolved resolveConfig() {
        try {
            Optional<StorageConfig> dbConfig = storageConfigRepository.findFirstByIsActiveTrueOrderByCreatedAtDesc();
            if (dbConfig.isPresent()) {
                StorageConfig cfg = dbConfig.get();
                return new StorageConfigResolved(
                        cfg.getStorageType(),
                        cfg.getProvider(),
                        cfg.getEndpointUrl(),
                        cfg.getBucketName() != null ? cfg.getBucketName() : defaultBucketName,
                        cfg.getRegion() != null ? cfg.getRegion() : defaultRegion,
                        cfg.getAccessKey(),
                        cfg.getSecretKey(),
                        cfg.getPathStyleAccess() != null ? cfg.getPathStyleAccess() : defaultPathStyleAccess
                );
            }
        } catch (Exception e) {
            log.debug("Usando configuração padrão de fallback: {}", e.getMessage());
        }

        return new StorageConfigResolved(
                "local".equalsIgnoreCase(defaultStorageType) ? StorageType.LOCAL : StorageType.S3,
                StorageProvider.SEAWEEDFS_LOCAL,
                defaultEndpointUrl,
                defaultBucketName,
                defaultRegion,
                defaultAccessKey,
                defaultSecretKey,
                defaultPathStyleAccess
        );
    }

    public record StorageConfigResolved(
            StorageType storageType,
            StorageProvider provider,
            String endpointUrl,
            String bucketName,
            String region,
            String accessKey,
            String secretKey,
            boolean pathStyleAccess
    ) {}
}
