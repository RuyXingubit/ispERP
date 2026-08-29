package br.dev.xb.isperp.service;

import br.dev.xb.isperp.entity.OutboxEvent;
import br.dev.xb.isperp.event.GenericDomainEvent;
import br.dev.xb.isperp.repository.OutboxEventRepository;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxDispatcherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private OutboxDispatcher outboxDispatcher;

    private OutboxEvent pendingEvent;
    private UUID eventId;

    @BeforeEach
    void setUp() {
        eventId = UuidCreatorUtils.generateUuidV7();
        pendingEvent = OutboxEvent.builder()
                .id(eventId)
                .eventType("WORK_ORDER_COMPLETED")
                .aggregateType("WorkOrder")
                .aggregateId("WO-12345")
                .payload("{\"status\":\"SUCCESS\"}")
                .status(OutboxEvent.OutboxStatus.PENDING)
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Deve despachar evento com sucesso e atualizar status para PUBLISHED")
    void shouldDispatchPendingEventSuccessfully() {
        when(outboxEventRepository.findByStatusOrderByCreatedAtAsc(eq(OutboxEvent.OutboxStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of(pendingEvent));

        outboxDispatcher.dispatchPendingEvents();

        assertEquals(OutboxEvent.OutboxStatus.PUBLISHED, pendingEvent.getStatus());
        assertNotNull(pendingEvent.getProcessedAt());
        assertNull(pendingEvent.getLastError());

        verify(applicationEventPublisher, times(1)).publishEvent(any(GenericDomainEvent.class));
        verify(outboxEventRepository, times(1)).save(pendingEvent);
    }

    @Test
    @DisplayName("Deve incrementar retry e manter PENDING caso ocorra erro transitório")
    void shouldIncrementRetryAndKeepPendingOnTransientError() {
        doThrow(new RuntimeException("Consumer temporary unavailable"))
                .when(applicationEventPublisher).publishEvent(any(GenericDomainEvent.class));

        outboxDispatcher.dispatchEvent(pendingEvent);

        assertEquals(OutboxEvent.OutboxStatus.PENDING, pendingEvent.getStatus());
        assertEquals(1, pendingEvent.getRetryCount());
        assertEquals("Consumer temporary unavailable", pendingEvent.getLastError());

        verify(outboxEventRepository, times(1)).save(pendingEvent);
    }

    @Test
    @DisplayName("Deve marcar evento como FAILED após atingir o número máximo de retentativas")
    void shouldMarkAsFailedWhenMaxRetriesReached() {
        pendingEvent.setRetryCount(4); // 4 retries já tentadas, a próxima será a 5ª (MAX_RETRIES)
        doThrow(new RuntimeException("Fatal error in consumer"))
                .when(applicationEventPublisher).publishEvent(any(GenericDomainEvent.class));

        outboxDispatcher.dispatchEvent(pendingEvent);

        assertEquals(OutboxEvent.OutboxStatus.FAILED, pendingEvent.getStatus());
        assertEquals(5, pendingEvent.getRetryCount());
        assertEquals("Fatal error in consumer", pendingEvent.getLastError());

        verify(outboxEventRepository, times(1)).save(pendingEvent);
    }
}
