package br.dev.xb.isperp.controller.backup;

import br.dev.xb.isperp.backup.SecurityMode;
import br.dev.xb.isperp.backup.StorageType;
import br.dev.xb.isperp.dto.backup.BackupDestinationResponse;
import br.dev.xb.isperp.dto.backup.BackupOverviewDto;
import br.dev.xb.isperp.mapper.DisasterRecoveryMapperImpl;
import br.dev.xb.isperp.service.backup.BackupStreamingPipelineService;
import br.dev.xb.isperp.service.backup.DisasterRecoveryService;
import br.dev.xb.isperp.service.backup.StorageTestResult;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = BackupDisasterRecoveryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(DisasterRecoveryMapperImpl.class)
@SuppressWarnings("null")
class BackupDisasterRecoveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DisasterRecoveryService disasterRecoveryService;

    @MockitoBean
    private BackupStreamingPipelineService pipelineService;

    @Test
    @DisplayName("GET /financial/backup/overview - Deve retornar visão geral do Disaster Recovery")
    void shouldReturnBackupOverview() throws Exception {
        BackupOverviewDto overview = BackupOverviewDto.builder()
                .hasActivePolicy(true)
                .securityMode(SecurityMode.MANAGED_RESCUE)
                .cronExpression("0 0 3 * * *")
                .retentionDays(30)
                .activeDestinationsCount(2)
                .totalBackupsCount(15)
                .lastBackupStatus("SUCCESS")
                .lastBackupAt(OffsetDateTime.now())
                .isDryRunVerified(true)
                .rescueKitDownloaded(true)
                .build();

        when(disasterRecoveryService.getOverview()).thenReturn(overview);

        mockMvc.perform(get("/financial/backup/overview")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasActivePolicy").value(true))
                .andExpect(jsonPath("$.cronExpression").value("0 0 3 * * *"))
                .andExpect(jsonPath("$.totalBackupsCount").value(15));
    }

    @Test
    @DisplayName("GET /financial/backup/destinations - Deve listar destinos cadastrados")
    void shouldListDestinations() throws Exception {
        UUID id = UuidCreatorUtils.generateUuidV7();
        BackupDestinationResponse destination = BackupDestinationResponse.builder()
                .id(id)
                .name("Wasabi Primary")
                .storageType(StorageType.S3_COMPATIBLE)
                .endpointUrl("https://s3.wasabisys.com")
                .bucketName("isperp-dr")
                .region("us-east-1")
                .isActive(true)
                .isPrimary(true)
                .createdAt(OffsetDateTime.now())
                .build();

        when(disasterRecoveryService.listDestinations()).thenReturn(List.of(destination));

        mockMvc.perform(get("/financial/backup/destinations")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Wasabi Primary"))
                .andExpect(jsonPath("$[0].storageType").value("S3_COMPATIBLE"));
    }

    @Test
    @DisplayName("POST /financial/backup/destinations/{id}/test - Deve testar conectividade")
    void shouldTestDestination() throws Exception {
        UUID id = UuidCreatorUtils.generateUuidV7();
        StorageTestResult result = StorageTestResult.builder()
                .success(true)
                .message("Bucket acessível e com permissão de escrita/leitura")
                .latencyMs(120)
                .build();

        when(disasterRecoveryService.testDestination(id)).thenReturn(result);

        mockMvc.perform(post("/financial/backup/destinations/{id}/test", id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.latencyMs").value(120));
    }

    @Test
    @DisplayName("GET /financial/backup/emergency-kit - Deve baixar o kit de resgate em markdown")
    void shouldDownloadEmergencyKit() throws Exception {
        String kitMarkdown = "# KIT DE RESGATE DE EMERGÊNCIA";
        when(disasterRecoveryService.generateEmergencyKitContent()).thenReturn(kitMarkdown);

        mockMvc.perform(get("/financial/backup/emergency-kit"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"KIT_RESGATE_EMERGENCIA_ISPERP.md\""))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_MARKDOWN))
                .andExpect(content().string(kitMarkdown));
    }
}
