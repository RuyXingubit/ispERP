package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.dto.*;
import br.dev.xb.isperp.service.FtthFusionService;
import br.dev.xb.isperp.service.FtthLightPathService;
import br.dev.xb.isperp.service.FtthTopologyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FtthTopologyController.class)
@AutoConfigureMockMvc(addFilters = false)
class FtthTopologyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FtthTopologyService topologyService;

    @MockitoBean
    private FtthFusionService fusionService;

    @MockitoBean
    private FtthLightPathService lightPathService;

    @Test
    @DisplayName("GET /api/ftth/ctos deve retornar lista de CTOs")
    void testGetAllCtos() throws Exception {
        FtthCtoResponse cto = FtthCtoResponse.builder()
                .id(UUID.randomUUID())
                .name("CTO-CENTRO-01")
                .totalPorts(16)
                .freePortsCount(14)
                .occupiedPortsCount(2)
                .latitude(new BigDecimal("-23.550520"))
                .longitude(new BigDecimal("-46.633308"))
                .status("ATIVA")
                .build();

        when(topologyService.getAllCtos()).thenReturn(List.of(cto));

        mockMvc.perform(get("/api/ftth/ctos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("CTO-CENTRO-01"))
                .andExpect(jsonPath("$[0].totalPorts").value(16));
    }

    @Test
    @DisplayName("POST /api/ftth/feasibility deve retornar viabilidade de atendimento")
    void testCheckFeasibility() throws Exception {
        FtthFeasibilityResponse response = FtthFeasibilityResponse.builder()
                .viable(true)
                .viableCtosCount(1)
                .nearbyCtos(List.of())
                .build();

        when(topologyService.calculateFeasibility(any())).thenReturn(response);

        String json = """
                {
                    "latitude": -23.550520,
                    "longitude": -46.633308,
                    "maxDistanceMeters": 150.0
                }
                """;

        mockMvc.perform(post("/api/ftth/feasibility")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viable").value(true))
                .andExpect(jsonPath("$.viableCtosCount").value(1));
    }
}
