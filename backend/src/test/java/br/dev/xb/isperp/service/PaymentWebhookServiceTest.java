package br.dev.xb.isperp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import br.dev.xb.isperp.entity.Invoice;
import br.dev.xb.isperp.entity.PaymentGatewayConfig;
import br.dev.xb.isperp.gateway.PaymentGateway;
import br.dev.xb.isperp.gateway.PaymentGatewayResolver;
import br.dev.xb.isperp.gateway.PaymentGatewayType;
import br.dev.xb.isperp.repository.PaymentTransactionRepository;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentWebhookServiceTest {

    @Mock
    private PaymentGatewayResolver gatewayResolver;

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private PaymentTransactionRepository transactionRepository;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private NfcomDecisionService nfcomDecisionService;

    @Mock
    private br.dev.xb.isperp.gateway.xingubit.XingubitPayGateway xingubitPayGateway;

    @Mock
    private br.dev.xb.isperp.repository.CustomerRepository customerRepository;

    @Mock
    private br.dev.xb.isperp.repository.InvoiceRepository invoiceRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PaymentWebhookService webhookService;

    private PaymentGatewayConfig sampleConfig;

    @BeforeEach
    void setUp() {
        sampleConfig = PaymentGatewayConfig.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .gatewayType(PaymentGatewayType.XINGUBIT_PAY)
                .build();
    }

    @Test
    @DisplayName("Deve processar webhook, localizar fatura e marcá-la como PAGA")
    void shouldProcessPaymentWebhook() {
        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(2);
            action.run();
            return true;
        }).when(idempotencyService).executeIdempotent(any(), any(), any());

        when(gatewayResolver.resolve(PaymentGatewayType.XINGUBIT_PAY))
                .thenReturn(new PaymentGatewayResolver.ResolvedGateway(paymentGateway, sampleConfig));

        when(paymentGateway.getGatewayType()).thenReturn(PaymentGatewayType.XINGUBIT_PAY);
        when(paymentGateway.processWebhook(any(), any(), eq(sampleConfig))).thenReturn("XB-TX-12345");

        UUID invoiceId = UuidCreatorUtils.generateUuidV7();
        Invoice sampleInvoice = Invoice.builder()
                .id(invoiceId)
                .amount(new BigDecimal("119.90"))
                .externalTransactionId("XB-TX-12345")
                .build();

        when(invoiceService.getInvoiceByExternalTransactionId("XB-TX-12345"))
                .thenReturn(Optional.of(sampleInvoice));

        Map<String, Object> payload = Map.of(
                "txid", "XB-TX-12345",
                "paidAmount", "119.90"
        );

        webhookService.processPaymentWebhook("XINGUBIT_PAY", payload, "signature123");

        verify(invoiceService, times(1)).markInvoiceAsPaid(invoiceId, new BigDecimal("119.90"), "PIX");
    }
}
