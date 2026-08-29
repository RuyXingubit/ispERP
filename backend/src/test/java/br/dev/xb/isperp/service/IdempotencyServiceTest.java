package br.dev.xb.isperp.service;

import br.dev.xb.isperp.entity.ProcessedEvent;
import br.dev.xb.isperp.repository.ProcessedEventRepository;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class IdempotencyServiceTest {

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @InjectMocks
    private IdempotencyService idempotencyService;

    @Test
    @DisplayName("Deve retornar false para evento ainda não processado")
    void shouldReturnFalseWhenEventNotProcessed() {
        UUID eventId = UuidCreatorUtils.generateUuidV7();
        String consumer = "BillingConsumer";

        when(processedEventRepository.existsByEventIdAndConsumerName(eventId, consumer)).thenReturn(false);

        assertFalse(idempotencyService.isAlreadyProcessed(eventId, consumer));
    }

    @Test
    @DisplayName("Deve retornar true para evento já processado")
    void shouldReturnTrueWhenEventAlreadyProcessed() {
        UUID eventId = UuidCreatorUtils.generateUuidV7();
        String consumer = "BillingConsumer";

        when(processedEventRepository.existsByEventIdAndConsumerName(eventId, consumer)).thenReturn(true);

        assertTrue(idempotencyService.isAlreadyProcessed(eventId, consumer));
    }

    @Test
    @DisplayName("Deve registrar evento como processado")
    void shouldMarkEventAsProcessed() {
        UUID eventId = UuidCreatorUtils.generateUuidV7();
        String consumer = "ContractConsumer";

        idempotencyService.markAsProcessed(eventId, consumer);

        verify(processedEventRepository, times(1)).save(any(ProcessedEvent.class));
    }

    @Test
    @DisplayName("Deve executar ação e marcar como processado quando evento é novo")
    void shouldExecuteActionWhenEventIsNew() {
        UUID eventId = UuidCreatorUtils.generateUuidV7();
        String consumer = "NotificationConsumer";
        AtomicInteger counter = new AtomicInteger(0);

        when(processedEventRepository.existsByEventIdAndConsumerName(eventId, consumer)).thenReturn(false);

        boolean executed = idempotencyService.executeIdempotent(eventId, consumer, counter::incrementAndGet);

        assertTrue(executed);
        assertEquals(1, counter.get());
        verify(processedEventRepository, times(1)).save(any(ProcessedEvent.class));
    }

    @Test
    @DisplayName("Deve ignorar ação e não registrar novamente quando evento já foi processado")
    void shouldSkipActionWhenEventAlreadyProcessed() {
        UUID eventId = UuidCreatorUtils.generateUuidV7();
        String consumer = "NotificationConsumer";
        AtomicInteger counter = new AtomicInteger(0);

        when(processedEventRepository.existsByEventIdAndConsumerName(eventId, consumer)).thenReturn(true);

        boolean executed = idempotencyService.executeIdempotent(eventId, consumer, counter::incrementAndGet);

        assertFalse(executed);
        assertEquals(0, counter.get());
        verify(processedEventRepository, never()).save(any(ProcessedEvent.class));
    }
}
