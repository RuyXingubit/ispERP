package br.dev.xb.isperp.service;

import br.dev.xb.isperp.entity.OutboxEvent;
import br.dev.xb.isperp.event.GenericDomainEvent;
import br.dev.xb.isperp.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.lang.NonNull;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxDispatcher {

    private static final int MAX_RETRIES = 5;
    private static final int BATCH_SIZE = 50;

    private final OutboxEventRepository outboxEventRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * Job agendado para processamento resiliente de eventos pendentes na Outbox.
     */
    @Scheduled(fixedDelayString = "${outbox.dispatcher.fixed-delay-ms:2000}")
    @Transactional
    public void dispatchPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatusOrderByCreatedAtAsc(
                OutboxEvent.OutboxStatus.PENDING,
                PageRequest.of(0, BATCH_SIZE)
        );

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Processando lote de {} eventos pendentes na Outbox", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            dispatchEvent(event);
        }
    }

    /**
     * Despacha um evento individual e atualiza seu estado de acordo com o resultado.
     */
    public void dispatchEvent(OutboxEvent event) {
        try {
            log.info("Despachando evento Outbox: id={}, type={}, aggregateId={}",
                    event.getId(), event.getEventType(), event.getAggregateId());

            GenericDomainEvent domainEvent = GenericDomainEvent.builder()
                    .eventId(event.getId())
                    .eventType(event.getEventType())
                    .aggregateType(event.getAggregateType())
                    .aggregateId(event.getAggregateId())
                    .payload(event.getPayload())
                    .occurredAt(event.getCreatedAt())
                    .build();

            applicationEventPublisher.publishEvent(domainEvent);

            event.setStatus(OutboxEvent.OutboxStatus.PUBLISHED);
            event.setProcessedAt(LocalDateTime.now());
            event.setLastError(null);
            outboxEventRepository.save(event);

            log.info("Evento Outbox {} publicado com sucesso", event.getId());
        } catch (Exception e) {
            log.error("Erro ao despachar evento Outbox {}: {}", event.getId(), e.getMessage(), e);

            int newRetryCount = (event.getRetryCount() != null ? event.getRetryCount() : 0) + 1;
            event.setRetryCount(newRetryCount);
            event.setLastError(e.getMessage());

            if (newRetryCount >= MAX_RETRIES) {
                event.setStatus(OutboxEvent.OutboxStatus.FAILED);
                log.warn("Evento Outbox {} atingiu o limite de retentativas ({}) e foi marcado como FAILED",
                        event.getId(), MAX_RETRIES);
            } else {
                event.setStatus(OutboxEvent.OutboxStatus.PENDING);
            }

            outboxEventRepository.save(event);
        }
    }
}
