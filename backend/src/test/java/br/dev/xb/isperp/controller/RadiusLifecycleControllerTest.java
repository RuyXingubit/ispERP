package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.dto.*;
import br.dev.xb.isperp.radius.RadiusBlockMode;
import br.dev.xb.isperp.scheduler.RadiusLifecycleScheduler;
import br.dev.xb.isperp.service.RadiusLifecycleService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RadiusLifecycleController.class)
@AutoConfigureMockMvc(addFilters = false)
class RadiusLifecycleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RadiusLifecycleService lifecycleService;

    @MockitoBean
    private RadiusLifecycleScheduler lifecycleScheduler;

    @Test
    @DisplayName("GET /api/radius/lifecycle/summary deve retornar resumo estatístico")
    void testGetSummary() throws Exception {
        RadiusLifecycleSummaryResponse summary = RadiusLifecycleSummaryResponse.builder()
                .totalPppoeUsers(150)
                .totalActiveUsers(140)
                .totalBlockedUsers(10)
                .toleranceDays(5)
                .autoBlockEnabled(true)
                .build();

        when(lifecycleService.getSummary()).thenReturn(summary);

        mockMvc.perform(get("/api/radius/lifecycle/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPppoeUsers").value(150))
                .andExpect(jsonPath("$.totalActiveUsers").value(140))
                .andExpect(jsonPath("$.totalBlockedUsers").value(10))
                .andExpect(jsonPath("$.toleranceDays").value(5));
    }

    @Test
    @DisplayName("POST /api/radius/lifecycle/action deve executar ação manual")
    void testExecuteManualAction() throws Exception {
        UUID contractId = UUID.randomUUID();
        RadiusManualActionResponse response = RadiusManualActionResponse.builder()
                .contractId(contractId)
                .username("cliente123")
                .actionApplied("BLOCK")
                .success(true)
                .message("Contrato bloqueado com sucesso")
                .build();

        when(lifecycleService.executeManualAction(any())).thenReturn(response);

        String json = """
                {
                    "contractId": "%s",
                    "action": "BLOCK",
                    "reason": "Bloqueio solicitado pelo suporte"
                }
                """.formatted(contractId);

        mockMvc.perform(post("/api/radius/lifecycle/action")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.actionApplied").value("BLOCK"));
    }

    @Test
    @DisplayName("POST /api/radius/lifecycle/run-autoblock deve disparar scheduler")
    void testRunAutoBlock() throws Exception {
        mockMvc.perform(post("/api/radius/lifecycle/run-autoblock"))
                .andExpect(status().isOk());

        verify(lifecycleScheduler).processAutoBlockRoutine();
    }
}
