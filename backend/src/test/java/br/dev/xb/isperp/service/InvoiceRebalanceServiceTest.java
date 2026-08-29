package br.dev.xb.isperp.service;

import br.dev.xb.isperp.entity.Contract;
import br.dev.xb.isperp.entity.Invoice;
import br.dev.xb.isperp.repository.ContractRepository;
import br.dev.xb.isperp.repository.InvoiceRepository;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceRebalanceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private DomainEventPublisher eventPublisher;

    @InjectMocks
    private InvoiceRebalanceService invoiceRebalanceService;

    private UUID contractId;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        contractId = UuidCreatorUtils.generateUuidV7();
        customerId = UuidCreatorUtils.generateUuidV7();
    }

    @Test
    @DisplayName("Deve detectar pagamento invertido e proteger fatura atrasada contra suspensão")
    void shouldDetectOutOfOrderPaymentAndProtectContract() {
        // Fatura de Janeiro em atraso
        Invoice januaryInvoice = Invoice.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .contractId(contractId)
                .customerId(customerId)
                .dueDate(LocalDate.of(2026, 1, 15))
                .status(Invoice.InvoiceStatus.OVERDUE)
                .amount(new BigDecimal("99.90"))
                .protectedAgainstSuspension(false)
                .build();

        // Fatura de Fevereiro (futura) que o cliente pagou por engano
        Invoice februaryInvoice = Invoice.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .contractId(contractId)
                .customerId(customerId)
                .dueDate(LocalDate.of(2026, 2, 15))
                .status(Invoice.InvoiceStatus.PAID)
                .amount(new BigDecimal("99.90"))
                .paidAt(LocalDateTime.now())
                .build();

        when(invoiceRepository.findByContractIdOrderByDueDateDesc(contractId)).thenReturn(List.of(januaryInvoice, februaryInvoice));

        boolean detected = invoiceRebalanceService.checkAndHandleOutOfOrderPayment(februaryInvoice);

        assertTrue(detected);
        assertTrue(januaryInvoice.getProtectedAgainstSuspension());
        verify(invoiceRepository, times(1)).save(januaryInvoice);
        verify(eventPublisher, times(1)).publish(any(br.dev.xb.isperp.event.DomainEvent.class));
    }

    @Test
    @DisplayName("Deve executar compensação cruzada gravando avisos explicativos fixos em ambas as faturas")
    void shouldExecuteCrossCreditRebalanceWithExplanatoryNotices() {
        UUID janId = UuidCreatorUtils.generateUuidV7();
        UUID febId = UuidCreatorUtils.generateUuidV7();

        Invoice janInvoice = Invoice.builder()
                .id(janId)
                .contractId(contractId)
                .customerId(customerId)
                .dueDate(LocalDate.of(2026, 1, 15))
                .status(Invoice.InvoiceStatus.OVERDUE)
                .amount(new BigDecimal("99.90"))
                .build();

        Invoice febInvoice = Invoice.builder()
                .id(febId)
                .contractId(contractId)
                .customerId(customerId)
                .dueDate(LocalDate.of(2026, 2, 15))
                .status(Invoice.InvoiceStatus.PAID)
                .paidAmount(new BigDecimal("99.90"))
                .paidAt(LocalDateTime.of(2026, 1, 20, 10, 0))
                .amount(new BigDecimal("99.90"))
                .build();

        Contract contract = Contract.builder()
                .id(contractId)
                .status(Contract.ContractStatus.SUSPENDED)
                .build();

        when(invoiceRepository.findById(febId)).thenReturn(Optional.of(febInvoice));
        when(invoiceRepository.findById(janId)).thenReturn(Optional.of(janInvoice));
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));

        invoiceRebalanceService.executeCrossCreditRebalance(febId, janId);

        // Fatura de Janeiro liquidada com crédito de Fevereiro
        assertEquals(Invoice.InvoiceStatus.PAID, janInvoice.getStatus());
        assertEquals("COMPENSACAO_CRUZADA", janInvoice.getPaymentMethod());
        assertEquals(febId, janInvoice.getPaidByCrossCreditId());
        assertNotNull(janInvoice.getRebalanceNotice());
        assertTrue(janInvoice.getRebalanceNotice().contains("quitada automaticamente"));

        // Fatura de Fevereiro reaberta sem juros
        assertEquals(Invoice.InvoiceStatus.PENDING, febInvoice.getStatus());
        assertNull(febInvoice.getPaidAt());
        assertNotNull(febInvoice.getRebalanceNotice());
        assertTrue(febInvoice.getRebalanceNotice().contains("reaberta para pagamento regular"));

        // Contrato restaurado para ACTIVE
        assertEquals(Contract.ContractStatus.ACTIVE, contract.getStatus());
    }
}
