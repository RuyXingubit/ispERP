package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.dto.AuditLogResponseDto;
import br.dev.xb.isperp.service.AuditLogService;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuditLogController.class)
@AutoConfigureMockMvc(addFilters = false)
@SuppressWarnings("null")
class AuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditLogService auditLogService;

    @Test
    @DisplayName("GET /api/admin/audit-logs deve retornar página de logs com status 200")
    void shouldReturnPagedAuditLogs() throws Exception {
        UUID logId = UuidCreatorUtils.generateUuidV7();
        UUID userId = UuidCreatorUtils.generateUuidV7();

        AuditLogResponseDto dto = AuditLogResponseDto.builder()
                .id(logId)
                .userId(userId)
                .userName("Maria Contabilidade")
                .userEmail("maria@provedor.com.br")
                .action("FINANCIAL_ENTRY_CREATED")
                .entityName("FINANCIAL")
                .entityId("entry-789")
                .details("{\"tipo\":\"CREDITO\",\"valor\":1500.00}")
                .createdAt(LocalDateTime.now())
                .build();

        when(auditLogService.searchLogs(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dto)));

        mockMvc.perform(get("/api/admin/audit-logs")
                        .param("entityName", "FINANCIAL")
                        .param("action", "ENTRY")
                        .param("page", "0")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(logId.toString()))
                .andExpect(jsonPath("$.content[0].userName").value("Maria Contabilidade"))
                .andExpect(jsonPath("$.content[0].action").value("FINANCIAL_ENTRY_CREATED"))
                .andExpect(jsonPath("$.content[0].entityName").value("FINANCIAL"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
