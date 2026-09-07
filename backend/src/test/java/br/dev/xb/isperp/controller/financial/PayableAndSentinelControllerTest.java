package br.dev.xb.isperp.controller.financial;

import br.dev.xb.isperp.dto.financial.ExpenseInstallmentDto;
import br.dev.xb.isperp.dto.financial.PayableInvoiceDto;
import br.dev.xb.isperp.dto.financial.SentinelAuditLogDto;
import br.dev.xb.isperp.entity.financial.PayableStatus;
import br.dev.xb.isperp.entity.financial.SentinelSeverity;
import br.dev.xb.isperp.mapper.FinancialDomainMapperImpl;
import br.dev.xb.isperp.service.financial.PayableInvoiceService;
import br.dev.xb.isperp.service.financial.SentinelWatchdogService;
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
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {PayableInvoiceController.class, SentinelAuditController.class})
@AutoConfigureMockMvc(addFilters = false)
@Import(FinancialDomainMapperImpl.class)
@SuppressWarnings("null")
class PayableAndSentinelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PayableInvoiceService payableInvoiceService;

    @MockitoBean
    private SentinelWatchdogService sentinelWatchdogService;

    @Test
    @DisplayName("GET /financial/payables - Deve listar contas a pagar")
    void shouldGetAllPayables() throws Exception {
        UUID id = UuidCreatorUtils.generateUuidV7();
        PayableInvoiceDto dto = PayableInvoiceDto.builder()
                .id(id)
                .supplierName("FiberHome Distribuidora")
                .description("Bobinas de Fibra Óptica 12FO")
                .totalAmount(new BigDecimal("18500.00"))
                .issueDate(LocalDate.of(2026, 8, 1))
                .status(PayableStatus.PENDING)
                .installments(Collections.emptyList())
                .build();

        when(payableInvoiceService.getAllPayables()).thenReturn(List.of(dto));

        mockMvc.perform(get("/financial/payables")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].supplierName").value("FiberHome Distribuidora"))
                .andExpect(jsonPath("$[0].totalAmount").value(18500.00));
    }

    @Test
    @DisplayName("POST /financial/payables/installments/{id}/pay - Deve liquidar parcela")
    void shouldPayInstallment() throws Exception {
        UUID id = UuidCreatorUtils.generateUuidV7();
        ExpenseInstallmentDto installment = ExpenseInstallmentDto.builder()
                .id(id)
                .installmentNumber(1)
                .amount(new BigDecimal("3083.33"))
                .paidAmount(new BigDecimal("3083.33"))
                .status(PayableStatus.PAID)
                .paymentMethod("PIX")
                .build();

        when(payableInvoiceService.payInstallment(id, new BigDecimal("3083.33"), "PIX", null)).thenReturn(installment);

        mockMvc.perform(post("/financial/payables/installments/{id}/pay", id)
                        .param("paidAmount", "3083.33")
                        .param("paymentMethod", "PIX")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paidAmount").value(3083.33));
    }

    @Test
    @DisplayName("GET /financial/sentinel/alerts - Deve listar alertas periciais do Sentinela")
    void shouldGetActiveSentinelAlerts() throws Exception {
        UUID id = UuidCreatorUtils.generateUuidV7();
        SentinelAuditLogDto alert = SentinelAuditLogDto.builder()
                .id(id)
                .auditType("RETENCAO_DINHEIRO_COLABORADOR")
                .severity(SentinelSeverity.HIGH)
                .title("Saldo em dinheiro retido por mais de 48h")
                .description("Colaborador retendo R$ 1.200 sem depósito bancário")
                .resolved(false)
                .build();

        when(sentinelWatchdogService.getActiveAuditAlerts()).thenReturn(List.of(alert));

        mockMvc.perform(get("/financial/sentinel/alerts")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].auditType").value("RETENCAO_DINHEIRO_COLABORADOR"))
                .andExpect(jsonPath("$[0].severity").value("HIGH"));
    }
}
