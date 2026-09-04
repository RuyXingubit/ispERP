package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.dto.FiscalRegimeTransitionResponse;
import br.dev.xb.isperp.fiscal.FiscalRegimeTransitionStatus;
import br.dev.xb.isperp.mapper.FiscalRegimeTransitionMapperImpl;
import br.dev.xb.isperp.service.FiscalRegimeTransitionService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FiscalRegimeTransitionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(FiscalRegimeTransitionMapperImpl.class)
@SuppressWarnings("null")
class FiscalRegimeTransitionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FiscalRegimeTransitionService regimeTransitionService;

    @Test
    @DisplayName("POST /fiscal/regimes/transitions - Deve agendar ou aplicar transição de regime tributário")
    void testScheduleOrApplyTransition() throws Exception {
        UUID transitionId = UuidCreatorUtils.generateUuidV7();
        UUID companyId = UuidCreatorUtils.generateUuidV7();

        FiscalRegimeTransitionResponse response = FiscalRegimeTransitionResponse.builder()
                .id(transitionId)
                .companyId(companyId)
                .previousRegime("SIMPLES_NACIONAL")
                .newRegime("LUCRO_PRESUMIDO")
                .effectiveDate(LocalDate.now().plusDays(30))
                .aliquotaIcms(BigDecimal.ZERO)
                .aliquotaPis(new BigDecimal("0.65"))
                .aliquotaCofins(new BigDecimal("3.00"))
                .aliquotaFust(new BigDecimal("0.65"))
                .aliquotaFunttel(new BigDecimal("0.50"))
                .status(FiscalRegimeTransitionStatus.SCHEDULED)
                .notes("Migração para Lucro Presumido para o próximo ano fiscal")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(regimeTransitionService.scheduleOrApply(any())).thenReturn(response);

        String payload = """
                {
                    "companyId": "%s",
                    "newRegime": "LUCRO_PRESUMIDO",
                    "effectiveDate": "%s",
                    "aliquotaIcms": 0.0,
                    "aliquotaPis": 0.65,
                    "aliquotaCofins": 3.00,
                    "aliquotaFust": 0.65,
                    "aliquotaFunttel": 0.50,
                    "notes": "Migração para Lucro Presumido para o próximo ano fiscal"
                }
                """.formatted(companyId, LocalDate.now().plusDays(30));

        mockMvc.perform(post("/fiscal/regimes/transitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transitionId.toString()))
                .andExpect(jsonPath("$.companyId").value(companyId.toString()))
                .andExpect(jsonPath("$.previousRegime").value("SIMPLES_NACIONAL"))
                .andExpect(jsonPath("$.newRegime").value("LUCRO_PRESUMIDO"))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
    }

    @Test
    @DisplayName("GET /fiscal/regimes/transitions - Deve retornar histórico de transições de regime")
    void testGetTransitionHistory() throws Exception {
        UUID transitionId = UuidCreatorUtils.generateUuidV7();
        UUID companyId = UuidCreatorUtils.generateUuidV7();

        FiscalRegimeTransitionResponse response = FiscalRegimeTransitionResponse.builder()
                .id(transitionId)
                .companyId(companyId)
                .previousRegime("SIMPLES_NACIONAL")
                .newRegime("LUCRO_PRESUMIDO")
                .effectiveDate(LocalDate.now())
                .status(FiscalRegimeTransitionStatus.APPLIED)
                .appliedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        when(regimeTransitionService.getHistory(eq(companyId))).thenReturn(List.of(response));

        mockMvc.perform(get("/fiscal/regimes/transitions")
                        .param("companyId", companyId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(transitionId.toString()))
                .andExpect(jsonPath("$[0].status").value("APPLIED"));
    }

    @Test
    @DisplayName("DELETE /fiscal/regimes/transitions/{id} - Deve cancelar agendamento de transição")
    void testCancelTransition() throws Exception {
        UUID transitionId = UuidCreatorUtils.generateUuidV7();
        UUID companyId = UuidCreatorUtils.generateUuidV7();

        FiscalRegimeTransitionResponse response = FiscalRegimeTransitionResponse.builder()
                .id(transitionId)
                .companyId(companyId)
                .previousRegime("SIMPLES_NACIONAL")
                .newRegime("LUCRO_REAL")
                .effectiveDate(LocalDate.now().plusMonths(2))
                .status(FiscalRegimeTransitionStatus.CANCELLED)
                .createdAt(LocalDateTime.now())
                .build();

        when(regimeTransitionService.cancelTransition(transitionId)).thenReturn(response);

        mockMvc.perform(delete("/fiscal/regimes/transitions/{id}", transitionId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transitionId.toString()))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("POST /fiscal/regimes/transitions/process-pending - Deve disparar processamento de pendentes")
    void testTriggerProcessPending() throws Exception {
        when(regimeTransitionService.applyPendingTransitions()).thenReturn(3);

        mockMvc.perform(post("/fiscal/regimes/transitions/process-pending")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(3));
    }
}
