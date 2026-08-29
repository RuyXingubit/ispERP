package br.dev.xb.isperp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import br.dev.xb.isperp.entity.OutboxEvent;
import br.dev.xb.isperp.event.DomainEvent;
import br.dev.xb.isperp.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class DomainEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObjectMapper objectMapper;

    /**
     * Publica um evento de domínio gravando-o atomicamente na Outbox e emitindo no contexto Spring.
     *
     * @param event Evento de domínio a ser publicado
     * @return O registro persistido na Outbox
     */
    @Transactional
    public OutboxEvent publish(@NonNull DomainEvent event) {
        log.info("Publicando evento de domínio: type={}, id={}, aggregateId={}",
                event.getEventType(), event.getEventId(), event.getAggregateId());

        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(event.getPayload());
        } catch (Exception e) {
            log.error("Erro ao serializar payload do evento {}: {}", event.getEventId(), e.getMessage());
            throw new RuntimeException("Falha ao serializar payload do evento", e);
        }

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .id(event.getEventId())
                .eventType(event.getEventType())
                .aggregateType(event.getAggregateType())
                .aggregateId(event.getAggregateId())
                .payload(jsonPayload)
                .status(OutboxEvent.OutboxStatus.PENDING)
                .retryCount(0)
                .createdAt(event.getOccurredAt() != null ? event.getOccurredAt() : LocalDateTime.now())
                .build();

        OutboxEvent saved = outboxEventRepository.save(outboxEvent);

        // Notifica listeners locais do Spring
        applicationEventPublisher.publishEvent(event);

        return saved;
    }
}
