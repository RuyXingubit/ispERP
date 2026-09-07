package br.dev.xb.isperp.controller.financial;

import br.dev.xb.isperp.dto.financial.WorkOrderFeeDto;
import br.dev.xb.isperp.entity.financial.FeeStatus;
import br.dev.xb.isperp.mapper.FinancialDomainMapperImpl;
import br.dev.xb.isperp.service.financial.WorkOrderFeeService;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = WorkOrderFeeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(FinancialDomainMapperImpl.class)
@SuppressWarnings("null")
class WorkOrderFeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkOrderFeeService workOrderFeeService;

    @Test
    @DisplayName("POST /financial/work-orders/{id}/assign-fee - Deve atribuir taxa padrão para OS")
    void shouldAssignStandardWorkOrderFee() throws Exception {
        UUID workOrderId = UuidCreatorUtils.generateUuidV7();
        WorkOrderFeeDto feeDto = WorkOrderFeeDto.builder()
                .workOrderId(workOrderId)
                .protocol("OS-2026-001")
                .customerName("Maria Souza")
                .serviceType("INSTALACAO")
                .standardFeeAmount(new BigDecimal("150.00"))
                .feeStatus(FeeStatus.BILLABLE)
                .build();

        when(workOrderFeeService.assignStandardFee(eq(workOrderId), any())).thenReturn(feeDto);

        mockMvc.perform(post("/financial/work-orders/{id}/assign-fee", workOrderId)
                        .param("amount", "150.00")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.protocol").value("OS-2026-001"))
                .andExpect(jsonPath("$.standardFeeAmount").value(150.00))
                .andExpect(jsonPath("$.feeStatus").value("BILLABLE"));
    }

    @Test
    @DisplayName("POST /financial/work-orders/waiver/request - Deve solicitar isenção de taxa")
    void shouldRequestWorkOrderFeeWaiver() throws Exception {
        UUID userId = UuidCreatorUtils.generateUuidV7();
        UUID workOrderId = UuidCreatorUtils.generateUuidV7();

        WorkOrderFeeDto feeDto = WorkOrderFeeDto.builder()
                .workOrderId(workOrderId)
                .protocol("OS-2026-002")
                .feeStatus(FeeStatus.PENDING_WAIVER_APPROVAL)
                .waiverReason("Cortesia por fidelidade")
                .build();

        when(workOrderFeeService.requestWaiver(eq(userId), any())).thenReturn(feeDto);

        String requestBody = """
                {
                    "workOrderId": "%s",
                    "waiverReason": "Cortesia por fidelidade"
                }
                """.formatted(workOrderId);

        mockMvc.perform(post("/financial/work-orders/waiver/request")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feeStatus").value("PENDING_WAIVER_APPROVAL"))
                .andExpect(jsonPath("$.waiverReason").value("Cortesia por fidelidade"));
    }

    @Test
    @DisplayName("GET /financial/work-orders/waiver/pending - Deve listar isenções pendentes de auditoria")
    void shouldGetPendingWorkOrderFeeWaivers() throws Exception {
        UUID workOrderId = UuidCreatorUtils.generateUuidV7();
        WorkOrderFeeDto feeDto = WorkOrderFeeDto.builder()
                .workOrderId(workOrderId)
                .protocol("OS-2026-003")
                .feeStatus(FeeStatus.PENDING_WAIVER_APPROVAL)
                .build();

        when(workOrderFeeService.getPendingWaiverAudits()).thenReturn(List.of(feeDto));

        mockMvc.perform(get("/financial/work-orders/waiver/pending")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].protocol").value("OS-2026-003"));
    }

    @Test
    @DisplayName("POST /financial/work-orders/{id}/waiver/audit - Deve auditar solicitação de isenção")
    void shouldAuditWorkOrderFeeWaiver() throws Exception {
        UUID auditorId = UuidCreatorUtils.generateUuidV7();
        UUID workOrderId = UuidCreatorUtils.generateUuidV7();

        WorkOrderFeeDto feeDto = WorkOrderFeeDto.builder()
                .workOrderId(workOrderId)
                .protocol("OS-2026-004")
                .feeStatus(FeeStatus.WAIVED_APPROVED)
                .build();

        when(workOrderFeeService.auditWaiver(eq(auditorId), eq(workOrderId), any())).thenReturn(feeDto);

        String requestBody = """
                {
                    "approved": true,
                    "notes": "Aprovado pelo CFO"
                }
                """;

        mockMvc.perform(post("/financial/work-orders/{id}/waiver/audit", workOrderId)
                        .header("X-User-Id", auditorId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feeStatus").value("WAIVED_APPROVED"));
    }
}
