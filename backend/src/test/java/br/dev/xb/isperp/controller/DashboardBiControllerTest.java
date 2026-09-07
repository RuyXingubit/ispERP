package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.dto.DashboardBiDTO;
import br.dev.xb.isperp.mapper.DashboardBiMapperImpl;
import br.dev.xb.isperp.service.DashboardBiService;
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
import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DashboardBiController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(DashboardBiMapperImpl.class)
@SuppressWarnings("null")
class DashboardBiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardBiService dashboardBiService;

    @Test
    @DisplayName("GET /bi/metrics - Deve retornar métricas consolidadas do provedor")
    void shouldReturnDashboardBiMetrics() throws Exception {
        DashboardBiDTO dto = DashboardBiDTO.builder()
                .mrr(new BigDecimal("125000.00"))
                .arr(new BigDecimal("1500000.00"))
                .arpu(new BigDecimal("99.90"))
                .overdueAmount(new BigDecimal("12450.50"))
                .defaultRate(new BigDecimal("4.85"))
                .pixConversionRate(new BigDecimal("68.20"))
                .totalReceivedMonth(new BigDecimal("112550.00"))
                .totalCustomers(1250)
                .activeContracts(1180)
                .suspendedContracts(45)
                .pendingInstallationContracts(25)
                .canceledContractsLast30Days(12)
                .churnRate(new BigDecimal("1.02"))
                .totalOnus(1200)
                .provisionedOnus(1180)
                .criticalSignalOnus(18)
                .totalNetworkDevices(14)
                .recentOverdueInvoices(Collections.emptyList())
                .criticalSignalAlerts(Collections.emptyList())
                .build();

        when(dashboardBiService.getDashboardMetrics()).thenReturn(dto);

        mockMvc.perform(get("/bi/metrics")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mrr").value(125000.00))
                .andExpect(jsonPath("$.totalCustomers").value(1250))
                .andExpect(jsonPath("$.activeContracts").value(1180));
    }
}
