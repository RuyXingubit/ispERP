package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.client.GeoCepClient;
import br.dev.xb.isperp.entity.ServiceRoute;
import br.dev.xb.isperp.service.RouteOptimizationService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GeoCepController.class)
@AutoConfigureMockMvc(addFilters = false)
@SuppressWarnings("null")
class GeoCepControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GeoCepClient geoCepClient;

    @MockitoBean
    private RouteOptimizationService routeOptimizationService;

    @Test
    @DisplayName("GET /geocep/cep/{cep} deve retornar dados de endereço georreferenciado")
    void testLookupCep() throws Exception {
        GeoCepClient.CepLookupResult result = GeoCepClient.CepLookupResult.builder()
                .cep("01310-100")
                .logradouro("Avenida Paulista")
                .bairro("Bela Vista")
                .localidade("São Paulo")
                .uf("SP")
                .latitude(new BigDecimal("-23.561414"))
                .longitude(new BigDecimal("-46.655881"))
                .build();

        when(geoCepClient.lookupCep("01310100", null)).thenReturn(result);

        mockMvc.perform(get("/geocep/cep/01310100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cep").value("01310-100"))
                .andExpect(jsonPath("$.logradouro").value("Avenida Paulista"))
                .andExpect(jsonPath("$.localidade").value("São Paulo"));
    }

    @Test
    @DisplayName("POST /geocep/routes/optimize deve criar rota otimizada")
    void testOptimizeRoute() throws Exception {
        ServiceRoute route = ServiceRoute.builder()
                .id(UUID.randomUUID())
                .code("ROTA-20260910-1042")
                .routeDate(LocalDate.of(2026, 9, 10))
                .totalDistanceKm(new BigDecimal("14.50"))
                .estimatedDurationMinutes(180)
                .status(ServiceRoute.RouteStatus.PLANNED)
                .build();

        when(routeOptimizationService.optimizeAndCreateRoute(any())).thenReturn(route);

        String json = """
                {
                    "routeDate": "2026-09-10",
                    "originLatitude": -23.550520,
                    "originLongitude": -46.633308,
                    "workOrderIds": ["0191c49b-7e62-7901-9257-234e44444444"]
                }
                """;

        mockMvc.perform(post("/geocep/routes/optimize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ROTA-20260910-1042"))
                .andExpect(jsonPath("$.status").value("PLANNED"));
    }
}
