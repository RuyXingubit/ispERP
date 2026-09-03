package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.dto.InvoiceResponse;
import br.dev.xb.isperp.api.dto.InvoiceStatus;
import br.dev.xb.isperp.api.dto.PayInvoiceRequest;
import br.dev.xb.isperp.entity.Contract;
import br.dev.xb.isperp.entity.Invoice;
import br.dev.xb.isperp.mapper.InvoiceMapper;
import br.dev.xb.isperp.service.BillingScheduler;
import br.dev.xb.isperp.service.ContractService;
import br.dev.xb.isperp.service.InvoiceService;
import br.dev.xb.isperp.util.UuidCreatorUtils;
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
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InvoiceController.class)
@AutoConfigureMockMvc(addFilters = false)
@SuppressWarnings("null")
class InvoiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @MockitoBean
    private InvoiceService invoiceService;

    @MockitoBean
    private ContractService contractService;

    @MockitoBean
    private BillingScheduler billingScheduler;

    @MockitoBean
    private InvoiceMapper invoiceMapper;

    private UUID invoiceId;
    private UUID contractId;
    private UUID customerId;
    private Invoice invoice;
    private InvoiceResponse invoiceResponse;

    @BeforeEach
    void setUp() {
        invoiceId = UuidCreatorUtils.generateUuidV7();
        contractId = UuidCreatorUtils.generateUuidV7();
        customerId = UuidCreatorUtils.generateUuidV7();

        invoice = Invoice.builder()
                .id(invoiceId)
                .contractId(contractId)
                .customerId(customerId)
                .amount(BigDecimal.valueOf(99.90))
                .dueDate(LocalDate.now().plusDays(10))
                .status(Invoice.InvoiceStatus.PENDING)
                .build();

        invoiceResponse = new InvoiceResponse();
        invoiceResponse.setId(invoiceId);
        invoiceResponse.setContractId(contractId);
        invoiceResponse.setCustomerId(customerId);
        invoiceResponse.setAmount(99.90);
        invoiceResponse.setDueDate(LocalDate.now().plusDays(10));
        invoiceResponse.setStatus(InvoiceStatus.PENDING);
    }

    @Test
    @DisplayName("GET /invoices - Deve listar todas as faturas")
    void shouldListAllInvoices() throws Exception {
        when(invoiceService.getAllInvoices()).thenReturn(List.of(invoice));
        when(invoiceMapper.toResponseList(any())).thenReturn(List.of(invoiceResponse));

        mockMvc.perform(get("/invoices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(invoiceId.toString()))
                .andExpect(jsonPath("$[0].amount").value(99.90))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    @DisplayName("GET /invoices/{id} - Deve retornar fatura por ID")
    void shouldGetInvoiceById() throws Exception {
        when(invoiceService.getInvoiceById(invoiceId)).thenReturn(Optional.of(invoice));
        when(invoiceMapper.toResponse(invoice)).thenReturn(invoiceResponse);

        mockMvc.perform(get("/invoices/{id}", invoiceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(invoiceId.toString()))
                .andExpect(jsonPath("$.contractId").value(contractId.toString()));
    }

    @Test
    @DisplayName("GET /invoices/{id} - Deve retornar 404 quando fatura não existir")
    void shouldReturn404WhenInvoiceNotFound() throws Exception {
        when(invoiceService.getInvoiceById(invoiceId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/invoices/{id}", invoiceId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /invoices/customer/{customerId} - Deve listar faturas do cliente")
    void shouldGetInvoicesByCustomerId() throws Exception {
        when(invoiceService.getInvoicesByCustomerId(customerId)).thenReturn(List.of(invoice));
        when(invoiceMapper.toResponseList(any())).thenReturn(List.of(invoiceResponse));

        mockMvc.perform(get("/invoices/customer/{customerId}", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerId").value(customerId.toString()));
    }

    @Test
    @DisplayName("GET /invoices/contract/{contractId} - Deve listar faturas do contrato")
    void shouldGetInvoicesByContractId() throws Exception {
        when(invoiceService.getInvoicesByContractId(contractId)).thenReturn(List.of(invoice));
        when(invoiceMapper.toResponseList(any())).thenReturn(List.of(invoiceResponse));

        mockMvc.perform(get("/invoices/contract/{contractId}", contractId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].contractId").value(contractId.toString()));
    }

    @Test
    @DisplayName("POST /invoices/generate/contract/{contractId} - Deve gerar fatura avulsa para contrato")
    void shouldGenerateInvoiceManually() throws Exception {
        Contract contract = Contract.builder().id(contractId).customerId(customerId).build();
        when(contractService.getContractById(contractId)).thenReturn(Optional.of(contract));
        when(invoiceService.createInvoiceForContract(eq(contract), any())).thenReturn(invoice);
        when(invoiceMapper.toResponse(invoice)).thenReturn(invoiceResponse);

        mockMvc.perform(post("/invoices/generate/contract/{contractId}", contractId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(invoiceId.toString()));
    }

    @Test
    @DisplayName("POST /invoices/{id}/pay - Deve liquidar fatura com sucesso")
    void shouldPayInvoice() throws Exception {
        PayInvoiceRequest request = new PayInvoiceRequest();
        request.setPaidAmount(99.90);
        request.setPaymentMethod("PIX");

        invoice.setStatus(Invoice.InvoiceStatus.PAID);
        invoiceResponse.setStatus(InvoiceStatus.PAID);

        when(invoiceService.markInvoiceAsPaid(eq(invoiceId), any(), eq("PIX"))).thenReturn(invoice);
        when(invoiceMapper.toResponse(invoice)).thenReturn(invoiceResponse);

        mockMvc.perform(post("/invoices/{id}/pay", invoiceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    @DisplayName("POST /invoices/{id}/cancel - Deve cancelar fatura")
    void shouldCancelInvoice() throws Exception {
        invoice.setStatus(Invoice.InvoiceStatus.CANCELED);
        invoiceResponse.setStatus(InvoiceStatus.CANCELLED);

        when(invoiceService.cancelInvoice(invoiceId)).thenReturn(invoice);
        when(invoiceMapper.toResponse(invoice)).thenReturn(invoiceResponse);

        mockMvc.perform(post("/invoices/{id}/cancel", invoiceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("POST /invoices/trigger-recurring-billing - Deve disparar rotina recorrente")
    void shouldTriggerRecurringBilling() throws Exception {
        mockMvc.perform(post("/invoices/trigger-recurring-billing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Rotina de faturamento recorrente executada com sucesso"));

        verify(billingScheduler).generateMonthlyInvoices();
    }
}
