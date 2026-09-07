package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.service.HierarchicalBillingService;
import br.dev.xb.isperp.service.InvoiceRebalanceService;
import br.dev.xb.isperp.service.TrustUnblockPolicyService;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BillingDunningController.class)
@AutoConfigureMockMvc(addFilters = false)
@SuppressWarnings("null")
class BillingDunningControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HierarchicalBillingService hierarchicalBillingService;

    @MockitoBean
    private InvoiceRebalanceService invoiceRebalanceService;

    @MockitoBean
    private TrustUnblockPolicyService trustUnblockPolicyService;

    @Test
    @DisplayName("POST /billing/dunning/process - Deve processar régua diária de corte")
    void shouldProcessDailyDunning() throws Exception {
        when(hierarchicalBillingService.processDailyDunning(any())).thenReturn(5);

        mockMvc.perform(post("/billing/dunning/process"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.suspendedCount").value(5));
    }

    @Test
    @DisplayName("POST /billing/dunning/rebalance/cross-credit - Deve executar compensação cruzada")
    void shouldExecuteCrossCreditRebalance() throws Exception {
        UUID futureId = UuidCreatorUtils.generateUuidV7();
        UUID overdueId = UuidCreatorUtils.generateUuidV7();

        doNothing().when(invoiceRebalanceService).executeCrossCreditRebalance(futureId, overdueId);

        mockMvc.perform(post("/billing/dunning/rebalance/cross-credit")
                        .param("futurePaidInvoiceId", futureId.toString())
                        .param("overdueUnpaidInvoiceId", overdueId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /billing/dunning/trust-unblock/bot - Deve avaliar desbloqueio automático via bot")
    void shouldRequestBotUnblock() throws Exception {
        UUID contractId = UuidCreatorUtils.generateUuidV7();
        TrustUnblockPolicyService.UnblockEvaluationResult result = TrustUnblockPolicyService.UnblockEvaluationResult.builder()
                .granted(true)
                .message("Desbloqueio temporário concedido por 24h.")
                .unblockType("BOT_AUTO")
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        when(trustUnblockPolicyService.requestBotAutoUnblock(contractId)).thenReturn(result);

        mockMvc.perform(post("/billing/dunning/trust-unblock/bot")
                        .param("contractId", contractId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.granted").value(true))
                .andExpect(jsonPath("$.unblockType").value("BOT_AUTO"));
    }

    @Test
    @DisplayName("POST /billing/dunning/trust-unblock/attendant - Deve processar desbloqueio manual de atendente")
    void shouldRequestAttendantUnblock() throws Exception {
        UUID contractId = UuidCreatorUtils.generateUuidV7();
        UUID attendantId = UuidCreatorUtils.generateUuidV7();
        TrustUnblockPolicyService.UnblockEvaluationResult result = TrustUnblockPolicyService.UnblockEvaluationResult.builder()
                .granted(true)
                .message("Desbloqueio manual liberado.")
                .unblockType("ATTENDANT_MANUAL")
                .expiresAt(LocalDateTime.now().plusHours(48))
                .build();

        when(trustUnblockPolicyService.requestAttendantManualUnblock(eq(contractId), eq(attendantId), eq("Cliente em viagem"))).thenReturn(result);

        mockMvc.perform(post("/billing/dunning/trust-unblock/attendant")
                        .param("contractId", contractId.toString())
                        .param("attendantUserId", attendantId.toString())
                        .param("reason", "Cliente em viagem"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.granted").value(true))
                .andExpect(jsonPath("$.unblockType").value("ATTENDANT_MANUAL"));
    }
}
