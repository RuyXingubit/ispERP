package br.dev.xb.isperp.config;

import br.dev.xb.isperp.controller.*;
import br.dev.xb.isperp.controller.backup.BackupDisasterRecoveryController;
import br.dev.xb.isperp.dto.DashboardBiDTO;
import br.dev.xb.isperp.entity.User;
import br.dev.xb.isperp.entity.UserRole;
import br.dev.xb.isperp.mapper.*;
import br.dev.xb.isperp.repository.FiscalCompanyRepository;
import br.dev.xb.isperp.repository.FiscalGatewayConfigRepository;
import br.dev.xb.isperp.repository.NetworkDeviceRepository;
import br.dev.xb.isperp.repository.NotificationConfigRepository;
import br.dev.xb.isperp.repository.PaymentGatewayConfigRepository;
import br.dev.xb.isperp.scheduler.FiscalAccountingScheduler;
import br.dev.xb.isperp.service.*;
import br.dev.xb.isperp.service.backup.BackupStreamingPipelineService;
import br.dev.xb.isperp.service.backup.DisasterRecoveryService;
import br.dev.xb.isperp.storage.FileStorageService;
import br.dev.xb.isperp.util.JwtUtil;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        DashboardBiController.class,
        UserController.class,
        BackupDisasterRecoveryController.class,
        StorageConfigController.class,
        NotificationConfigController.class,
        PaymentGatewayConfigController.class,
        AuditLogController.class,
        NasController.class,
        NetworkDeviceController.class,
        FiscalController.class,
        FileStorageController.class
})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@SuppressWarnings("null")
class RbacAndEndpointSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private DashboardBiService dashboardBiService;

    @MockitoBean
    private DashboardBiMapper dashboardBiMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserMapper userMapper;

    @MockitoBean
    private DisasterRecoveryService disasterRecoveryService;

    @MockitoBean
    private BackupStreamingPipelineService pipelineService;

    @MockitoBean
    private DisasterRecoveryMapper disasterRecoveryMapper;

    @MockitoBean
    private StorageConfigService storageConfigService;

    @MockitoBean
    private NotificationConfigRepository notificationConfigRepository;

    @MockitoBean
    private PaymentGatewayConfigRepository paymentGatewayConfigRepository;

    @MockitoBean
    private AuditLogService auditLogService;

    @MockitoBean
    private NasService nasService;

    @MockitoBean
    private NetworkDeviceRepository networkDeviceRepository;

    @MockitoBean
    private NetworkDeviceMapper networkDeviceMapper;

    @MockitoBean
    private FiscalService fiscalService;

    @MockitoBean
    private FiscalCompanyRepository fiscalCompanyRepository;

    @MockitoBean
    private FiscalGatewayConfigRepository fiscalGatewayConfigRepository;

    @MockitoBean
    private FiscalMapper fiscalMapper;

    @MockitoBean
    private ConvenioIcms115Service convenioIcms115Service;

    @MockitoBean
    private FiscalAccountingScheduler fiscalAccountingScheduler;

    @MockitoBean
    private FileStorageService fileStorageService;

    @BeforeEach
    void setUpJwtTokens() {
        for (String role : List.of("ADMIN", "TECHNICIAN", "ATTENDANT", "FINANCIAL", "CFO", "DIRECTOR", "SUPPORT_N2")) {
            String token = "token-" + role;
            String username = role.toLowerCase() + "@provedor.com.br";
            lenient().when(jwtUtil.extractUsername(token)).thenReturn(username);
            lenient().when(jwtUtil.extractRole(token)).thenReturn(role);
            lenient().when(jwtUtil.validateToken(token, username)).thenReturn(true);
        }
    }

    // =========================================================================
    // 1. BI Dashboard (/bi/metrics)
    // =========================================================================

    @Test
    @DisplayName("Segurança: GET /bi/metrics deve bloquear requisição anônima (403 Forbidden)")
    void shouldBlockAnonymousBiMetricsAccess() throws Exception {
        mockMvc.perform(get("/bi/metrics"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Segurança: GET /bi/metrics deve bloquear perfil TECHNICIAN (403 Forbidden)")
    void shouldBlockTechnicianFromBiMetrics() throws Exception {
        mockMvc.perform(get("/bi/metrics")
                        .header("Authorization", "Bearer token-TECHNICIAN"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Regressão: GET /bi/metrics deve permitir perfil ADMIN (200 OK)")
    void shouldAllowAdminToAccessBiMetrics() throws Exception {
        when(dashboardBiService.getDashboardMetrics()).thenReturn(DashboardBiDTO.builder().build());
        mockMvc.perform(get("/bi/metrics")
                        .header("Authorization", "Bearer token-ADMIN"))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // 2. Gestão de Usuários e Papéis (/users/**)
    // =========================================================================

    @Test
    @DisplayName("Segurança: PATCH /users/{id}/role deve bloquear escalada de privilégio por TECHNICIAN (403 Forbidden)")
    void shouldBlockPrivilegeEscalationByNonAdmin() throws Exception {
        UUID targetUserId = UuidCreatorUtils.generateUuidV7();
        mockMvc.perform(patch("/users/" + targetUserId + "/role")
                        .header("Authorization", "Bearer token-TECHNICIAN")
                        .param("role", "ADMIN"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Segurança: GET /users deve bloquear listagem de colaboradores por ATTENDANT (403 Forbidden)")
    void shouldBlockAttendantFromListingUsers() throws Exception {
        mockMvc.perform(get("/users")
                        .header("Authorization", "Bearer token-ATTENDANT"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Regressão: ADMIN deve conseguir gerenciar usuários e alterar papéis (200 OK)")
    void shouldAllowAdminToManageUsers() throws Exception {
        UUID targetUserId = UuidCreatorUtils.generateUuidV7();
        User updatedUser = User.builder().id(targetUserId).role(UserRole.ADMIN).build();
        when(userService.updateUserRole(targetUserId, UserRole.ADMIN)).thenReturn(updatedUser);

        mockMvc.perform(patch("/users/" + targetUserId + "/role")
                        .header("Authorization", "Bearer token-ADMIN")
                        .param("role", "ADMIN"))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // 3. Kit de Resgate e Backup (/financial/backup/**)
    // =========================================================================

    @Test
    @DisplayName("Segurança: GET /financial/backup/emergency-kit deve bloquear download da chave AES-256 por TECHNICIAN (403 Forbidden)")
    void shouldBlockNonAdminFromDownloadingEmergencyKit() throws Exception {
        mockMvc.perform(get("/financial/backup/emergency-kit")
                        .header("Authorization", "Bearer token-TECHNICIAN"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Regressão: ADMIN deve conseguir baixar o Kit de Resgate de Emergência (200 OK)")
    void shouldAllowAdminToDownloadEmergencyKit() throws Exception {
        when(disasterRecoveryService.generateEmergencyKitContent()).thenReturn("# KIT DE RESGATE");

        mockMvc.perform(get("/financial/backup/emergency-kit")
                        .header("Authorization", "Bearer token-ADMIN"))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // 4. Storage de Arquivos (/files/**)
    // =========================================================================

    @Test
    @DisplayName("Segurança: POST /files deve bloquear upload anônimo de arquivos (403 Forbidden)")
    void shouldBlockAnonymousFileUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "foto.jpg", MediaType.IMAGE_JPEG_VALUE, "img".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/files").file(file))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Regressão: Colaborador autenticado (TECHNICIAN) deve conseguir fazer upload de foto em POST /files (200 OK)")
    void shouldAllowAuthenticatedUserToUploadFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "foto.jpg", MediaType.IMAGE_JPEG_VALUE, "img".getBytes(StandardCharsets.UTF_8));
        when(fileStorageService.store(anyString(), anyString(), any())).thenReturn("uuid.jpg");
        when(fileStorageService.getFileUrl("uuid.jpg")).thenReturn("/api/files/uuid.jpg");

        mockMvc.perform(multipart("/files")
                        .file(file)
                        .header("Authorization", "Bearer token-TECHNICIAN"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Regressão: GET /files/{filename} deve permitir recuperação de arquivo armazenado (200 OK)")
    void shouldAllowFileDownload() throws Exception {
        when(fileStorageService.recover("comprovante.jpg"))
                .thenReturn(new ByteArrayResource("img".getBytes(StandardCharsets.UTF_8)));

        mockMvc.perform(get("/files/comprovante.jpg"))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // 5. Configurações Sensíveis (Storage S3, WhatsApp, Gateways, NAS, OLT, Fiscal, AuditLogs)
    // =========================================================================

    @Test
    @DisplayName("Segurança: Endpoints administrativos críticos devem bloquear perfil ATTENDANT com 403 Forbidden")
    void shouldBlockAttendantFromSensitiveConfigEndpoints() throws Exception {
        String auth = "Bearer token-ATTENDANT";
        mockMvc.perform(get("/storage/config").header("Authorization", auth)).andExpect(status().isForbidden());
        mockMvc.perform(get("/notifications/configs").header("Authorization", auth)).andExpect(status().isForbidden());
        mockMvc.perform(get("/payment-gateways").header("Authorization", auth)).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/audit-logs").header("Authorization", auth)).andExpect(status().isForbidden());
        mockMvc.perform(get("/radius/nas").header("Authorization", auth)).andExpect(status().isForbidden());
        mockMvc.perform(get("/network-devices").header("Authorization", auth)).andExpect(status().isForbidden());
        mockMvc.perform(get("/fiscal/company").header("Authorization", auth)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Regressão: ADMIN deve ter acesso normal aos endpoints administrativos de configuração (200 OK)")
    void shouldAllowAdminToAccessSensitiveConfigEndpoints() throws Exception {
        String auth = "Bearer token-ADMIN";
        when(notificationConfigRepository.findAll()).thenReturn(List.of());
        when(paymentGatewayConfigRepository.findAll()).thenReturn(List.of());
        when(auditLogService.searchLogs(any(), any(), any(), any(), any(), any())).thenReturn(Page.empty());
        when(nasService.getAllNas()).thenReturn(List.of());
        when(networkDeviceRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/notifications/configs").header("Authorization", auth)).andExpect(status().isOk());
        mockMvc.perform(get("/payment-gateways").header("Authorization", auth)).andExpect(status().isOk());
        mockMvc.perform(get("/admin/audit-logs").header("Authorization", auth)).andExpect(status().isOk());
        mockMvc.perform(get("/radius/nas").header("Authorization", auth)).andExpect(status().isOk());
        mockMvc.perform(get("/network-devices").header("Authorization", auth)).andExpect(status().isOk());
    }
}
