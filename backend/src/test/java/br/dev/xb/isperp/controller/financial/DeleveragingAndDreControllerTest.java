package br.dev.xb.isperp.controller.financial;

import br.dev.xb.isperp.dto.financial.AccountingMethod;
import br.dev.xb.isperp.dto.financial.DeleveragingProjectionDto;
import br.dev.xb.isperp.dto.financial.DreReportDto;
import br.dev.xb.isperp.mapper.FinancialDomainMapperImpl;
import br.dev.xb.isperp.service.financial.DeleveragingEngineService;
import br.dev.xb.isperp.service.financial.DreReportService;
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
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {DeleveragingController.class, DreReportController.class})
@AutoConfigureMockMvc(addFilters = false)
@Import(FinancialDomainMapperImpl.class)
@SuppressWarnings("null")
class DeleveragingAndDreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeleveragingEngineService deleveragingEngineService;

    @MockitoBean
    private DreReportService dreReportService;

    @Test
    @DisplayName("GET /financial/deleveraging/projection - Deve retornar projeção de 36 meses")
    void shouldGetDeleveragingProjection() throws Exception {
        DeleveragingProjectionDto projection = DeleveragingProjectionDto.builder()
                .startingCashBalance(new BigDecimal("50000.00"))
                .worstMonthProjectedBalance(new BigDecimal("-12000.00"))
                .breakEvenYearMonth("2027-11")
                .monthsUntilFreedom(18)
                .timeline(Collections.emptyList())
                .build();

        when(deleveragingEngineService.calculate36MonthsProjection()).thenReturn(projection);

        mockMvc.perform(get("/financial/deleveraging/projection")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startingCashBalance").value(50000.00))
                .andExpect(jsonPath("$.breakEvenYearMonth").value("2027-11"));
    }

    @Test
    @DisplayName("GET /financial/reports/dre - Deve gerar DRE consolidado")
    void shouldGetDreReport() throws Exception {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);

        DreReportDto dre = DreReportDto.builder()
                .periodStart(start)
                .periodEnd(end)
                .accountingMethod(AccountingMethod.ACCRUAL)
                .grossRevenue(new BigDecimal("150000.00"))
                .taxDeductions(new BigDecimal("15000.00"))
                .netRevenue(new BigDecimal("135000.00"))
                .directCostsInterconnection(new BigDecimal("35000.00"))
                .contributionMargin(new BigDecimal("100000.00"))
                .totalOpex(new BigDecimal("40000.00"))
                .ebitda(new BigDecimal("60000.00"))
                .ebitdaMarginPercentage(new BigDecimal("44.44"))
                .build();

        when(dreReportService.generateDre(eq(start), eq(end), any(AccountingMethod.class))).thenReturn(dre);

        mockMvc.perform(get("/financial/reports/dre")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-01-31")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grossRevenue").value(150000.00))
                .andExpect(jsonPath("$.ebitda").value(60000.00));
    }
}
