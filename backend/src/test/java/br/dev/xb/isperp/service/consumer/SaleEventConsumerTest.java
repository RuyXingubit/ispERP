package br.dev.xb.isperp.service.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import br.dev.xb.isperp.entity.Contract;
import br.dev.xb.isperp.entity.Customer;
import br.dev.xb.isperp.event.DomainEvent;
import br.dev.xb.isperp.event.GenericDomainEvent;
import br.dev.xb.isperp.repository.ContractRepository;
import br.dev.xb.isperp.repository.CustomerRepository;
import br.dev.xb.isperp.service.DomainEventPublisher;
import br.dev.xb.isperp.service.IdempotencyService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class SaleEventConsumerTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @Mock
    private IdempotencyService idempotencyService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private SaleEventConsumer saleEventConsumer;

    private UUID saleId;
    private UUID planId;
    private GenericDomainEvent saleSubmittedEvent;

    @BeforeEach
    void setUp() {
        saleId = UuidCreatorUtils.generateUuidV7();
        planId = UuidCreatorUtils.generateUuidV7();

        Map<String, Object> payload = new HashMap<>();
        payload.put("saleId", saleId.toString());
        payload.put("planId", planId.toString());
        payload.put("planName", "Fibra 600 Mega");
        payload.put("monthlyFee", "119.90");
        payload.put("customerName", "Maria Oliveira");
        payload.put("customerCpf", "12345678909");
        payload.put("customerEmail", "maria@email.com");
        payload.put("customerPhone", "11999998888");
        payload.put("installationAddress", "Rua Augusta, 500");
        payload.put("city", "São Paulo");
        payload.put("state", "SP");
        payload.put("zipCode", "01305100");
        payload.put("preferredDueDate", 15);

        saleSubmittedEvent = GenericDomainEvent.builder()
                .eventId(UuidCreatorUtils.generateUuidV7())
                .eventType("SALE_SUBMITTED")
                .aggregateType("Sale")
                .aggregateId(saleId.toString())
                .payload(payload)
                .build();
    }

    @Test
    @DisplayName("Deve consumir SALE_SUBMITTED, criar Customer, criar Contract e emitir CONTRACT_CREATED")
    void shouldProcessSaleSubmittedEvent() {
        // Mock idempotency execution
        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(2);
            action.run();
            return true;
        }).when(idempotencyService).executeIdempotent(any(), any(), any());

        when(customerRepository.findByCpf("12345678909")).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenAnswer(i -> {
            Customer c = i.getArgument(0);
            if (c.getId() == null) c.setId(UuidCreatorUtils.generateUuidV7());
            return c;
        });

        when(contractRepository.save(any(Contract.class))).thenAnswer(i -> {
            Contract ct = i.getArgument(0);
            if (ct.getId() == null) ct.setId(UuidCreatorUtils.generateUuidV7());
            return ct;
        });

        saleEventConsumer.handleSaleSubmittedEvent(saleSubmittedEvent);

        verify(customerRepository, times(1)).save(any(Customer.class));
        verify(contractRepository, times(1)).save(any(Contract.class));
        verify(domainEventPublisher, times(1)).publish(argThat(event -> 
            "CONTRACT_CREATED".equals(event.getEventType()) && "Contract".equals(event.getAggregateType())
        ));
    }

    @Test
    @DisplayName("Deve ignorar eventos que não sejam SALE_SUBMITTED")
    void shouldIgnoreOtherEventTypes() {
        GenericDomainEvent otherEvent = GenericDomainEvent.builder()
                .eventId(UuidCreatorUtils.generateUuidV7())
                .eventType("CUSTOMER_LOGGED_IN")
                .build();

        saleEventConsumer.handleSaleSubmittedEvent(otherEvent);

        verifyNoInteractions(idempotencyService);
        verifyNoInteractions(customerRepository);
        verifyNoInteractions(contractRepository);
    }
}
