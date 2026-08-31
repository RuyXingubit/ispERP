package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.dto.*;
import br.dev.xb.isperp.monitoring.IncidentSeverity;
import br.dev.xb.isperp.monitoring.IncidentStatus;
import br.dev.xb.isperp.monitoring.IncidentType;
import br.dev.xb.isperp.service.FtthCorrelationEngine;
import br.dev.xb.isperp.service.OltTelemetryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FtthMonitoringController.class)
@AutoConfigureMockMvc(addFilters = false)
class FtthMonitoringControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OltTelemetryService telemetryService;

    @MockitoBean
    private FtthCorrelationEngine correlationEngine;

    @Test
    @DisplayName("GET /api/ftth/monitoring/summary deve retornar resumo do NOC")
    void testGetMonitoringSummary() throws Exception {
        NocMonitoringSummaryResponse summary = NocMonitoringSummaryResponse.builder()
                .totalOlts(2)
                .totalPonPorts(32)
                .activePonPorts(30)
                .totalOnus(1200)
                .onlineOnus(1180)
                .losOnus(20)
                .globalHealthPercentage(98.3)
                .activeIncidentsCount(1)
                .criticalIncidentsCount(1)
                .activeIncidents(List.of())
                .build();

        when(correlationEngine.getMonitoringSummary()).thenReturn(summary);

        mockMvc.perform(get("/api/ftth/monitoring/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOlts").value(2))
                .andExpect(jsonPath("$.totalOnus").value(1200))
                .andExpect(jsonPath("$.globalHealthPercentage").value(98.3));
    }

    @Test
    @DisplayName("GET /api/ftth/monitoring/incidents/active deve retornar incidentes ativos")
    void testGetActiveIncidents() throws Exception {
        FtthIncidentResponse incident = FtthIncidentResponse.builder()
                .id(UUID.randomUUID())
                .title("Rompimento Troncal Provável: GPON 0/1/1")
                .incidentType(IncidentType.FIBER_CUT_PROBABLE)
                .severity(IncidentSeverity.CRITICAL)
                .status(IncidentStatus.ACTIVE)
                .affectedCustomersCount(16)
                .build();

        when(correlationEngine.getActiveIncidents()).thenReturn(List.of(incident));

        mockMvc.perform(get("/api/ftth/monitoring/incidents/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Rompimento Troncal Provável: GPON 0/1/1"))
                .andExpect(jsonPath("$[0].severity").value("CRITICAL"));
    }
}
