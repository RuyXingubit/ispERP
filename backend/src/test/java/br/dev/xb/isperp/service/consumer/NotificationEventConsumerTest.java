package br.dev.xb.isperp.service.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import br.dev.xb.isperp.entity.Customer;
import br.dev.xb.isperp.event.GenericDomainEvent;
import br.dev.xb.isperp.repository.CustomerRepository;
import br.dev.xb.isperp.service.EmailNotificationService;
import br.dev.xb.isperp.service.IdempotencyService;
import br.dev.xb.isperp.service.WhatsAppNotificationService;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class NotificationEventConsumerTest {

    @Mock
    private EmailNotificationService emailService;

    @Mock
    private WhatsAppNotificationService whatsAppService;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private CustomerRepository customerRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private NotificationEventConsumer consumer;

    private UUID customerId;
    private Customer customer;

    @BeforeEach
    void setUp() {
        customerId = UuidCreatorUtils.generateUuidV7();
        customer = Customer.builder()
                .id(customerId)
                .name("Maria Silva")
                .phone("93991234567")
                .email("maria.silva@provedor.dev.br")
                .cpf("12345678901")
                .build();

        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(2);
            action.run();
            return true;
        }).when(idempotencyService).executeIdempotent(any(), any(), any());
    }

    @Test
    @DisplayName("Deve enviar WhatsApp informando agendamento de retirada ao consumir REMOVAL_ORDER_GENERATED")
    void shouldSendScheduledWarningOnRemovalOrderGenerated() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        Map<String, Object> payload = new HashMap<>();
        payload.put("customerId", customerId.toString());
        payload.put("contractNumber", "CTR-2026-001");
        payload.put("scheduledDate", "2026-09-10");
        payload.put("scheduledPeriod", "MANHA");

        GenericDomainEvent event = GenericDomainEvent.builder()
                .eventId(UuidCreatorUtils.generateUuidV7())
                .eventType("REMOVAL_ORDER_GENERATED")
                .aggregateType("WorkOrder")
                .aggregateId(UuidCreatorUtils.generateUuidV7().toString())
                .payload(payload)
                .build();

        consumer.handleDomainEvent(event);

        verify(whatsAppService, times(1)).sendTextMessage(
                eq(customerId),
                eq("93991234567"),
                contains("Aviso de Recolhimento de Equipamento"),
                eq("REMOVAL_ORDER_SCHEDULED")
        );
    }

    @Test
    @DisplayName("Deve enviar comprovante de devolução de comodato ao consumir REMOVAL_ORDER_COMPLETED")
    void shouldSendReceiptOnRemovalOrderCompleted() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        Map<String, Object> payload = new HashMap<>();
        payload.put("customerId", customerId.toString());
        payload.put("workOrderId", UuidCreatorUtils.generateUuidV7().toString());

        GenericDomainEvent event = GenericDomainEvent.builder()
                .eventId(UuidCreatorUtils.generateUuidV7())
                .eventType("REMOVAL_ORDER_COMPLETED")
                .aggregateType("WorkOrder")
                .aggregateId(UuidCreatorUtils.generateUuidV7().toString())
                .payload(payload)
                .build();

        consumer.handleDomainEvent(event);

        verify(whatsAppService, times(1)).sendTextMessage(
                eq(customerId),
                eq("93991234567"),
                contains("Comprovante de Devolução de Equipamento"),
                eq("REMOVAL_ORDER_RECEIPT")
        );
    }

    @Test
    @DisplayName("Deve enviar notificação extrajudicial com débito ao consumir LEGAL_COLLECTION_RECORD_CREATED")
    void shouldSendLegalWarningOnLegalCollectionRecordCreated() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        Map<String, Object> payload = new HashMap<>();
        payload.put("customerId", customerId.toString());
        payload.put("totalClaimAmount", "620.00");
        payload.put("equipmentIndemnityAmount", "420.00");
        payload.put("unsuccessReason", "CLIENTE_RECUSOU_ENTREGA");

        GenericDomainEvent event = GenericDomainEvent.builder()
                .eventId(UuidCreatorUtils.generateUuidV7())
                .eventType("LEGAL_COLLECTION_RECORD_CREATED")
                .aggregateType("LegalCollectionRecord")
                .aggregateId(UuidCreatorUtils.generateUuidV7().toString())
                .payload(payload)
                .build();

        consumer.handleDomainEvent(event);

        verify(whatsAppService, times(1)).sendTextMessage(
                eq(customerId),
                eq("93991234567"),
                contains("Notificação Extrajudicial / Cobrança Jurídica"),
                eq("LEGAL_COLLECTION_WARNING")
        );
    }

    @Test
    @DisplayName("Deve enviar fatura PIX ao consumir INVOICE_GENERATED")
    void shouldSendPixInvoiceOnInvoiceGenerated() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("customerId", customerId.toString());
        payload.put("customerEmail", "maria.silva@provedor.dev.br");
        payload.put("customerPhone", "93991234567");
        payload.put("customerName", "Maria Silva");
        payload.put("amount", "99.90");
        payload.put("dueDate", "2026-09-15");
        payload.put("pixCopiaECola", "00020126...5802BR");
        payload.put("pixQrCodeUrl", "https://pix.com/qr.png");

        GenericDomainEvent event = GenericDomainEvent.builder()
                .eventId(UuidCreatorUtils.generateUuidV7())
                .eventType("INVOICE_GENERATED")
                .aggregateType("Invoice")
                .aggregateId(UuidCreatorUtils.generateUuidV7().toString())
                .payload(payload)
                .build();

        consumer.handleDomainEvent(event);

        verify(emailService, times(1)).sendEmail(eq("maria.silva@provedor.dev.br"), anyString(), anyString());
        verify(whatsAppService, times(1)).sendPixInvoice(eq(customerId), eq("93991234567"), eq("Maria Silva"), eq(new BigDecimal("99.90")), eq("2026-09-15"), eq("00020126...5802BR"), eq("https://pix.com/qr.png"));
    }
}
