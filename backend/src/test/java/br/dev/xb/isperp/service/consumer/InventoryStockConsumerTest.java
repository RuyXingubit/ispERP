package br.dev.xb.isperp.service.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import br.dev.xb.isperp.event.GenericDomainEvent;
import br.dev.xb.isperp.service.DomainEventPublisher;
import br.dev.xb.isperp.service.IdempotencyService;
import br.dev.xb.isperp.service.InventoryService;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryStockConsumerTest {

    @Mock
    private InventoryService inventoryService;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @Mock
    private IdempotencyService idempotencyService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private InventoryStockConsumer inventoryStockConsumer;

    private GenericDomainEvent contractCreatedEvent;
    private UUID contractId;

    @BeforeEach
    void setUp() {
        contractId = UuidCreatorUtils.generateUuidV7();

        Map<String, Object> payload = new HashMap<>();
        payload.put("contractId", contractId.toString());

        contractCreatedEvent = GenericDomainEvent.builder()
                .eventId(UuidCreatorUtils.generateUuidV7())
                .eventType("CONTRACT_CREATED")
                .aggregateType("Contract")
                .aggregateId(contractId.toString())
                .payload(payload)
                .build();
    }

    @Test
    @DisplayName("Deve verificar estoque e emitir evento STOCK_VERIFIED")
    void shouldCheckStockAndEmitEvent() {
        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(2);
            action.run();
            return true;
        }).when(idempotencyService).executeIdempotent(any(), any(), any());

        when(inventoryService.checkAndReserveInstallationMaterials(contractId))
                .thenReturn(Collections.emptyList());

        inventoryStockConsumer.handleContractCreated(contractCreatedEvent);

        verify(inventoryService, times(1)).checkAndReserveInstallationMaterials(contractId);
        verify(domainEventPublisher, times(1)).publish(argThat(event ->
                "STOCK_VERIFIED".equals(event.getEventType())
        ));
    }
}
