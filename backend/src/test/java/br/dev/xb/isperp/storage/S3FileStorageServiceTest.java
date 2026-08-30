package br.dev.xb.isperp.storage;

import br.dev.xb.isperp.entity.StorageConfig;
import br.dev.xb.isperp.repository.StorageConfigRepository;
import br.dev.xb.isperp.service.StorageConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3FileStorageServiceTest {

    @Mock
    private LocalFileStorageService localFileStorageService;

    @Mock
    private StorageConfigRepository storageConfigRepository;

    @Mock
    private StorageConfigService storageConfigService;

    @Mock
    private S3Client s3Client;

    private S3FileStorageService s3FileStorageService;

    @BeforeEach
    void setUp() {
        s3FileStorageService = new S3FileStorageService(
                localFileStorageService,
                storageConfigRepository,
                storageConfigService
        );
    }

    @Test
    @DisplayName("Deve delegar para LocalFileStorageService quando o storageType for LOCAL")
    void shouldDelegateToLocalWhenStorageTypeIsLocal() {
        StorageConfig localConfig = StorageConfig.builder()
                .storageType(StorageType.LOCAL)
                .provider(StorageProvider.LOCAL_DISK)
                .build();

        when(storageConfigRepository.findFirstByIsActiveTrueOrderByCreatedAtDesc())
                .thenReturn(Optional.of(localConfig));

        ByteArrayInputStream stream = new ByteArrayInputStream("conteudo".getBytes(StandardCharsets.UTF_8));
        when(localFileStorageService.store("arquivo.pdf", "application/pdf", stream))
                .thenReturn("01918a00-0000-7000-8000-000000000001.pdf");

        String result = s3FileStorageService.store("arquivo.pdf", "application/pdf", stream);
        assertThat(result).isEqualTo("01918a00-0000-7000-8000-000000000001.pdf");
        verify(localFileStorageService).store("arquivo.pdf", "application/pdf", stream);

        when(localFileStorageService.recover("01918a00-0000-7000-8000-000000000001.pdf"))
                .thenReturn(new ByteArrayResource("conteudo".getBytes(StandardCharsets.UTF_8)));

        Resource resource = s3FileStorageService.recover("01918a00-0000-7000-8000-000000000001.pdf");
        assertThat(resource).isNotNull();
        verify(localFileStorageService).recover("01918a00-0000-7000-8000-000000000001.pdf");

        s3FileStorageService.delete("01918a00-0000-7000-8000-000000000001.pdf");
        verify(localFileStorageService).delete("01918a00-0000-7000-8000-000000000001.pdf");
    }

    @Test
    @DisplayName("Deve armazenar arquivo no S3 com sucesso")
    void shouldStoreFileInS3Successfully() {
        StorageConfig s3Config = StorageConfig.builder()
                .storageType(StorageType.S3)
                .provider(StorageProvider.SEAWEEDFS_LOCAL)
                .endpointUrl("http://localhost:8333")
                .bucketName("isperp-files")
                .region("us-east-1")
                .build();

        when(storageConfigRepository.findFirstByIsActiveTrueOrderByCreatedAtDesc())
                .thenReturn(Optional.of(s3Config));

        when(storageConfigService.buildS3Client(any(), any(), any(), any(), anyBoolean()))
                .thenReturn(s3Client);

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        ByteArrayInputStream stream = new ByteArrayInputStream("imagem-termo".getBytes(StandardCharsets.UTF_8));
        String filename = s3FileStorageService.store("termo.png", "image/png", stream);

        assertThat(filename).endsWith(".png");
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("Deve recuperar arquivo do S3 com sucesso")
    void shouldRecoverFileFromS3Successfully() {
        StorageConfig s3Config = StorageConfig.builder()
                .storageType(StorageType.S3)
                .provider(StorageProvider.AWS_S3)
                .endpointUrl("https://s3.us-east-1.amazonaws.com")
                .bucketName("isperp-files")
                .region("us-east-1")
                .build();

        when(storageConfigRepository.findFirstByIsActiveTrueOrderByCreatedAtDesc())
                .thenReturn(Optional.of(s3Config));

        when(storageConfigService.buildS3Client(any(), any(), any(), any(), anyBoolean()))
                .thenReturn(s3Client);

        byte[] contentBytes = "documento-recuperado".getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream bais = new ByteArrayInputStream(contentBytes);
        AbortableInputStream abortableInputStream = AbortableInputStream.create(bais);
        GetObjectResponse getResponse = GetObjectResponse.builder().contentLength((long) contentBytes.length).build();
        ResponseInputStream<GetObjectResponse> responseStream = new ResponseInputStream<>(getResponse, abortableInputStream);

        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseStream);

        Resource resource = s3FileStorageService.recover("doc.pdf");
        assertThat(resource).isNotNull();
        assertThat(resource.getFilename()).isEqualTo("doc.pdf");
        verify(s3Client).getObject(any(GetObjectRequest.class));
    }

    @Test
    @DisplayName("Deve retornar URL padronizada para o arquivo")
    void shouldReturnStandardFileUrl() {
        String url = s3FileStorageService.getFileUrl("arquivo123.jpg");
        assertThat(url).isEqualTo("/api/files/arquivo123.jpg");
    }
}
