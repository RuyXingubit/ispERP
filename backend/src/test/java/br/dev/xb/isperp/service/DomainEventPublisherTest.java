package br.dev.xb.isperp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DomainEventPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private DomainEventPublisher domainEventPublisher;

    private GenericDomainEvent sampleEvent;
    private UUID sampleEventId;

    @BeforeEach
    void setUp() {
        sampleEventId = UuidCreatorUtils.generateUuidV7();
        sampleEvent = GenericDomainEvent.builder()
                .eventId(sampleEventId)
                .eventType("SALE_SUBMITTED")
                .aggregateType("Sale")
                .aggregateId("SALE-001")
                .payload(Map.of("customerName", "Ana Santos", "plan", "Fibra 500M"))
                .build();
    }

    @Test
    @DisplayName("Deve serializar e persistir evento na Outbox com status PENDING e emitir evento Spring")
    void shouldPersistToOutboxAndPublishSpringEvent() {
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OutboxEvent published = domainEventPublisher.publish(sampleEvent);

        assertNotNull(published);
        assertEquals(sampleEventId, published.getId());
        assertEquals("SALE_SUBMITTED", published.getEventType());
        assertEquals("Sale", published.getAggregateType());
        assertEquals("SALE-001", published.getAggregateId());
        assertEquals(OutboxEvent.OutboxStatus.PENDING, published.getStatus());
        assertTrue(published.getPayload().contains("Ana Santos"));

        // Verifica que o evento foi enviado para o ApplicationEventPublisher
        verify(applicationEventPublisher, times(1)).publishEvent(sampleEvent);
        verify(outboxEventRepository, times(1)).save(any(OutboxEvent.class));
    }

    @Test
    @DisplayName("Deve lançar exceção caso ocorra erro na serialização do payload")
    void shouldThrowExceptionWhenSerializationFails() throws Exception {
        ObjectMapper failingMapper = mock(ObjectMapper.class);
        DomainEventPublisher publisherWithFailingMapper = new DomainEventPublisher(
                outboxEventRepository, applicationEventPublisher, failingMapper
        );

        when(failingMapper.writeValueAsString(any())).thenThrow(new RuntimeException("Serialization error"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> publisherWithFailingMapper.publish(sampleEvent));
        assertTrue(ex.getMessage().contains("Falha ao serializar payload"));

        verify(outboxEventRepository, never()).save(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }
}
