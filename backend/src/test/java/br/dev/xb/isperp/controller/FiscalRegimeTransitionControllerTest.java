package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.dto.FiscalRegimeTransitionRequest;
import br.dev.xb.isperp.dto.FiscalRegimeTransitionResponse;
import br.dev.xb.isperp.fiscal.FiscalRegimeTransitionStatus;
import br.dev.xb.isperp.service.FiscalRegimeTransitionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FiscalRegimeTransitionController.class)
@AutoConfigureMockMvc(addFilters = false)
@SuppressWarnings("null")
class FiscalRegimeTransitionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private FiscalRegimeTransitionService transitionService;

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("POST /fiscal/regimes/transitions - Deve agendar ou aplicar transição com sucesso")
    void shouldScheduleOrApplyTransition() throws Exception {
        UUID transitionId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        FiscalRegimeTransitionRequest request = FiscalRegimeTransitionRequest.builder()
                .companyId(companyId)
                .newRegime("LUCRO_PRESUMIDO")
                .effectiveDate(LocalDate.now().plusMonths(1))
                .aliquotaIcms(new BigDecimal("18.00"))
                .build();

        FiscalRegimeTransitionResponse response = FiscalRegimeTransitionResponse.builder()
                .id(transitionId)
                .companyId(companyId)
                .previousRegime("SIMPLES_NACIONAL")
                .newRegime("LUCRO_PRESUMIDO")
                .effectiveDate(LocalDate.now().plusMonths(1))
                .status(FiscalRegimeTransitionStatus.SCHEDULED)
                .build();

        when(transitionService.scheduleOrApply(any(FiscalRegimeTransitionRequest.class))).thenReturn(response);

        mockMvc.perform(post("/fiscal/regimes/transitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transitionId.toString()))
                .andExpect(jsonPath("$.newRegime").value("LUCRO_PRESUMIDO"))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
    }

    @Test
    @DisplayName("GET /fiscal/regimes/transitions - Deve retornar histórico de transições")
    void shouldGetHistory() throws Exception {
        UUID companyId = UUID.randomUUID();
        FiscalRegimeTransitionResponse response = FiscalRegimeTransitionResponse.builder()
                .id(UUID.randomUUID())
                .companyId(companyId)
                .previousRegime("SIMPLES_NACIONAL")
                .newRegime("LUCRO_PRESUMIDO")
                .status(FiscalRegimeTransitionStatus.APPLIED)
                .build();

        when(transitionService.getHistory(any())).thenReturn(List.of(response));

        mockMvc.perform(get("/fiscal/regimes/transitions")
                        .param("companyId", companyId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].previousRegime").value("SIMPLES_NACIONAL"))
                .andExpect(jsonPath("$[0].status").value("APPLIED"));
    }

    @Test
    @DisplayName("DELETE /fiscal/regimes/transitions/{id} - Deve cancelar transição")
    void shouldCancelTransition() throws Exception {
        UUID transitionId = UUID.randomUUID();
        FiscalRegimeTransitionResponse response = FiscalRegimeTransitionResponse.builder()
                .id(transitionId)
                .status(FiscalRegimeTransitionStatus.CANCELLED)
                .build();

        when(transitionService.cancelTransition(transitionId)).thenReturn(response);

        mockMvc.perform(delete("/fiscal/regimes/transitions/{id}", transitionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
