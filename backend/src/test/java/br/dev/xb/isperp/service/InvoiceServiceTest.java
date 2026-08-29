package br.dev.xb.isperp.service;

import br.dev.xb.isperp.entity.Contract;
import br.dev.xb.isperp.entity.Customer;
import br.dev.xb.isperp.entity.Invoice;
import br.dev.xb.isperp.entity.PaymentGatewayConfig;
import br.dev.xb.isperp.event.DomainEvent;
import br.dev.xb.isperp.gateway.PaymentGateway;
import br.dev.xb.isperp.gateway.PaymentGatewayResolver;
import br.dev.xb.isperp.gateway.PaymentGatewayType;
import br.dev.xb.isperp.gateway.dto.CreateChargeResponse;
import br.dev.xb.isperp.repository.CustomerRepository;
import br.dev.xb.isperp.repository.InvoiceRepository;
import br.dev.xb.isperp.repository.PaymentTransactionRepository;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private PaymentTransactionRepository transactionRepository;

    @Mock
    private PaymentGatewayResolver gatewayResolver;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @Mock
    private PaymentGateway paymentGateway;

    @InjectMocks
    private InvoiceService invoiceService;

    private Contract sampleContract;
    private Customer sampleCustomer;
    private PaymentGatewayConfig sampleConfig;
    private UUID contractId;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        contractId = UuidCreatorUtils.generateUuidV7();
        customerId = UuidCreatorUtils.generateUuidV7();

        sampleCustomer = Customer.builder()
                .id(customerId)
                .name("Ruy Barbosa")
                .cpf("12345678909")
                .email("ruy@xingubit.com.br")
                .phone("11999998888")
                .build();

        sampleContract = Contract.builder()
                .id(contractId)
                .customerId(customerId)
                .contractNumber("CTR-2026001")
                .monthlyFee(new BigDecimal("119.90"))
                .dueDay(10)
                .build();

        sampleConfig = PaymentGatewayConfig.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .gatewayType(PaymentGatewayType.XINGUBIT_PAY)
                .build();
    }

    @Test
    @DisplayName("Deve emitir fatura com sucesso e publicar evento INVOICE_GENERATED")
    void shouldCreateInvoiceForContract() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(sampleCustomer));
        when(gatewayResolver.resolve(PaymentGatewayType.XINGUBIT_PAY))
                .thenReturn(new PaymentGatewayResolver.ResolvedGateway(paymentGateway, sampleConfig));

        CreateChargeResponse chargeResponse = CreateChargeResponse.builder()
                .externalTransactionId("XB-123456789")
                .gatewayType("XINGUBIT_PAY")
                .amount(new BigDecimal("119.90"))
                .pixCopiaECola("00020126580014br.gov.bcb.pix...")
                .pixQrCodeUrl("https://pay.xingubit.com.br/qrcode/123")
                .build();

        when(paymentGateway.createCharge(any(), eq(sampleConfig))).thenReturn(chargeResponse);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

        LocalDate dueDate = LocalDate.now().plusDays(10);
        Invoice invoice = invoiceService.createInvoiceForContract(sampleContract, dueDate);

        assertNotNull(invoice);
        assertEquals(Invoice.InvoiceStatus.PENDING, invoice.getStatus());
        assertEquals("XB-123456789", invoice.getExternalTransactionId());
        assertEquals("XINGUBIT_PAY", invoice.getGatewayType());

        verify(domainEventPublisher, times(1)).publish(any(DomainEvent.class));
    }

    @Test
    @DisplayName("Deve marcar fatura como PAGA e emitir evento INVOICE_PAID")
    void shouldMarkInvoiceAsPaid() {
        UUID invoiceId = UuidCreatorUtils.generateUuidV7();
        Invoice pendingInvoice = Invoice.builder()
                .id(invoiceId)
                .contractId(contractId)
                .customerId(customerId)
                .amount(new BigDecimal("119.90"))
                .status(Invoice.InvoiceStatus.PENDING)
                .build();

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(pendingInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

        Invoice paidInvoice = invoiceService.markInvoiceAsPaid(invoiceId, new BigDecimal("119.90"), "PIX");

        assertEquals(Invoice.InvoiceStatus.PAID, paidInvoice.getStatus());
        assertNotNull(paidInvoice.getPaidAt());
        verify(domainEventPublisher, times(1)).publish(any(DomainEvent.class));
    }
}
