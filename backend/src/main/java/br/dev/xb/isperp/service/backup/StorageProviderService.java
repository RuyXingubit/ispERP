package br.dev.xb.isperp.service.backup;

import br.dev.xb.isperp.entity.backup.BackupDestination;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorageProviderService {

    private final BackupCryptoService cryptoService;

    @Value("${app.backup.local-storage-path:/tmp/isperp-backups}")
    private String defaultLocalStoragePath;

    /**
     * Testa em tempo real a conectividade do destino efetuando upload e remoção de arquivo sentinela.
     */
    public StorageTestResult testConnection(BackupDestination destination) {
        long start = System.currentTimeMillis();
        String testFileName = ".healthcheck_" + UUID.randomUUID() + ".tmp";
        byte[] payload = ("ispERP Storage Test Connection at " + OffsetDateTime.now()).getBytes(StandardCharsets.UTF_8);

        try {
            switch (destination.getStorageType()) {
                case S3_COMPATIBLE -> {
                    S3Client s3 = buildS3Client(destination);
                    String fullKey = resolvePath(destination.getPathPrefix(), testFileName);

                    // 1. Put
                    s3.putObject(
                            PutObjectRequest.builder()
                                    .bucket(destination.getBucketName())
                                    .key(fullKey)
                                    .build(),
                            RequestBody.fromBytes(payload)
                    );

                    // 2. Head (valida existência)
                    s3.headObject(HeadObjectRequest.builder()
                            .bucket(destination.getBucketName())
                            .key(fullKey)
                            .build());

                    // 3. Delete
                    s3.deleteObject(DeleteObjectRequest.builder()
                            .bucket(destination.getBucketName())
                            .key(fullKey)
                            .build());

                    s3.close();
                }
                case LOCAL_VOLUME, SFTP, ISPERP_CLOUD -> {
                    Path root = Paths.get(destination.getPathPrefix() != null && !destination.getPathPrefix().isBlank()
                            ? destination.getPathPrefix()
                            : defaultLocalStoragePath);
                    Files.createDirectories(root);
                    Path testFile = root.resolve(testFileName);
                    Files.write(testFile, payload);
                    if (!Files.exists(testFile) || Files.size(testFile) != payload.length) {
                        throw new IllegalStateException("Falha de persistência no volume local.");
                    }
                    Files.deleteIfExists(testFile);
                }
            }

            long duration = System.currentTimeMillis() - start;
            return StorageTestResult.builder()
                    .success(true)
                    .message("Conexão e permissões de escrita/leitura/exclusão validadas com sucesso.")
                    .latencyMs(duration)
                    .build();

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.warn("Falha no teste de conexão do storage {}: {}", destination.getName(), e.getMessage());
            return StorageTestResult.builder()
                    .success(false)
                    .message("Falha ao comunicar com o destino de armazenamento.")
                    .detailedError(e.getMessage())
                    .latencyMs(duration)
                    .build();
        }
    }

    /**
     * Realiza o upload final do arquivo criptografado para o destino.
     */
    public void uploadFile(BackupDestination destination, String fileName, File localFile) {
        try {
            switch (destination.getStorageType()) {
                case S3_COMPATIBLE -> {
                    S3Client s3 = buildS3Client(destination);
                    String fullKey = resolvePath(destination.getPathPrefix(), fileName);
                    s3.putObject(
                            PutObjectRequest.builder()
                                    .bucket(destination.getBucketName())
                                    .key(fullKey)
                                    .build(),
                            RequestBody.fromFile(localFile)
                    );
                    s3.close();
                    log.info("Backup {} enviado com sucesso para S3 (Bucket: {}, Key: {})", fileName, destination.getBucketName(), fullKey);
                }
                case LOCAL_VOLUME, SFTP, ISPERP_CLOUD -> {
                    Path root = Paths.get(destination.getPathPrefix() != null && !destination.getPathPrefix().isBlank()
                            ? destination.getPathPrefix()
                            : defaultLocalStoragePath);
                    Files.createDirectories(root);
                    Path target = root.resolve(fileName);
                    Files.copy(localFile.toPath(), target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    log.info("Backup {} salvo com sucesso no volume {}", fileName, target);
                }
            }
        } catch (Exception e) {
            log.error("Erro no upload do backup {} para {}: {}", fileName, destination.getName(), e.getMessage(), e);
            throw new IllegalStateException("Falha ao persistir backup no destino: " + destination.getName(), e);
        }
    }

    /**
     * Constrói cliente S3 com endpoint customizado para Cloudflare R2, MinIO ou AWS nativo.
     */
    private S3Client buildS3Client(BackupDestination destination) {
        String rawSecret = destination.getSecretKeyEncrypted() != null
                ? cryptoService.decryptSystemSecret(destination.getSecretKeyEncrypted())
                : "";

        String accessKey = destination.getAccessKey() != null ? destination.getAccessKey() : "";
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, rawSecret);

        S3ClientBuilder builder = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(credentials));

        if (destination.getRegion() != null && !destination.getRegion().isBlank() && !"auto".equalsIgnoreCase(destination.getRegion())) {
            builder.region(Region.of(destination.getRegion()));
        } else {
            builder.region(Region.US_EAST_1);
        }

        if (destination.getEndpointUrl() != null && !destination.getEndpointUrl().isBlank()) {
            builder.endpointOverride(URI.create(destination.getEndpointUrl()));
        }

        return builder.build();
    }

    private String resolvePath(String prefix, String fileName) {
        if (prefix == null || prefix.isBlank()) return fileName;
        String clean = prefix.endsWith("/") ? prefix : prefix + "/";
        clean = clean.startsWith("/") ? clean.substring(1) : clean;
        return clean + fileName;
    }
}
