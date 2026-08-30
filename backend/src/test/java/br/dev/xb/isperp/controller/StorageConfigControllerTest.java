package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.dto.StorageConfigRequest;
import br.dev.xb.isperp.dto.StorageConfigResponse;
import br.dev.xb.isperp.dto.StorageConnectionTestResponse;
import br.dev.xb.isperp.service.StorageConfigService;
import br.dev.xb.isperp.storage.StorageProvider;
import br.dev.xb.isperp.storage.StorageType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = StorageConfigController.class)
@AutoConfigureMockMvc(addFilters = false)
@SuppressWarnings("null")
class StorageConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private StorageConfigService storageConfigService;

    @Test
    @DisplayName("GET /storage/config - Deve retornar configuração ativa")
    void shouldReturnActiveStorageConfig() throws Exception {
        StorageConfigResponse response = StorageConfigResponse.builder()
                .id(UUID.randomUUID())
                .storageType(StorageType.S3)
                .provider(StorageProvider.SEAWEEDFS_LOCAL)
                .endpointUrl("http://localhost:8333")
                .bucketName("isperp-files")
                .region("us-east-1")
                .isActive(true)
                .build();

        when(storageConfigService.getActiveConfig()).thenReturn(response);

        mockMvc.perform(get("/storage/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("SEAWEEDFS_LOCAL"))
                .andExpect(jsonPath("$.bucketName").value("isperp-files"))
                .andExpect(jsonPath("$.endpointUrl").value("http://localhost:8333"));
    }

    @Test
    @DisplayName("PUT /storage/config - Deve atualizar configuração com sucesso")
    void shouldUpdateStorageConfig() throws Exception {
        StorageConfigRequest request = StorageConfigRequest.builder()
                .storageType(StorageType.S3)
                .provider(StorageProvider.AWS_S3)
                .endpointUrl("https://s3.us-east-1.amazonaws.com")
                .bucketName("meu-bucket")
                .region("sa-east-1")
                .accessKey("AKIA123")
                .secretKey("SecretKey123")
                .pathStyleAccess(false)
                .isActive(true)
                .build();

        StorageConfigResponse response = StorageConfigResponse.builder()
                .id(UUID.randomUUID())
                .storageType(StorageType.S3)
                .provider(StorageProvider.AWS_S3)
                .endpointUrl("https://s3.us-east-1.amazonaws.com")
                .bucketName("meu-bucket")
                .region("sa-east-1")
                .isActive(true)
                .build();

        when(storageConfigService.saveOrUpdate(any(StorageConfigRequest.class))).thenReturn(response);

        mockMvc.perform(put("/storage/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("AWS_S3"))
                .andExpect(jsonPath("$.bucketName").value("meu-bucket"));
    }

    @Test
    @DisplayName("POST /storage/config/test - Deve executar teste de conectividade")
    void shouldTestStorageConnection() throws Exception {
        StorageConfigRequest request = StorageConfigRequest.builder()
                .storageType(StorageType.S3)
                .provider(StorageProvider.CLOUDFLARE_R2)
                .endpointUrl("https://12345.r2.cloudflarestorage.com")
                .bucketName("isperp-r2")
                .region("auto")
                .pathStyleAccess(true)
                .isActive(true)
                .build();

        StorageConnectionTestResponse testResponse = StorageConnectionTestResponse.builder()
                .success(true)
                .message("Conexão S3 estabelecida com sucesso!")
                .details("Cloudflare R2 operacional.")
                .latencyMs(45)
                .build();

        when(storageConfigService.testConnection(any(StorageConfigRequest.class))).thenReturn(testResponse);

        mockMvc.perform(post("/storage/config/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Conexão S3 estabelecida com sucesso!"))
                .andExpect(jsonPath("$.latencyMs").value(45));
    }
}
