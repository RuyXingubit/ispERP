package br.dev.xb.isperp.service;

import br.dev.xb.isperp.entity.ProcessedEvent;
import br.dev.xb.isperp.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class IdempotencyService {

    private final ProcessedEventRepository processedEventRepository;

    /**
     * Verifica se o evento já foi processado pelo consumidor informado.
     */
    public boolean isAlreadyProcessed(@NonNull UUID eventId, @NonNull String consumerName) {
        return processedEventRepository.existsByEventIdAndConsumerName(eventId, consumerName);
    }

    /**
     * Marca o evento como processado com sucesso pelo consumidor informado.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsProcessed(@NonNull UUID eventId, @NonNull String consumerName) {
        log.debug("Marcando evento como processado: eventId={}, consumer={}", eventId, consumerName);

        ProcessedEvent processedEvent = ProcessedEvent.builder()
                .eventId(eventId)
                .consumerName(consumerName)
                .processedAt(LocalDateTime.now())
                .build();

        processedEventRepository.save(processedEvent);
    }

    /**
     * Executa uma ação de forma estritamente idempotente. Se o evento já foi processado, ignora a execução.
     *
     * @param eventId Identificador do evento
     * @param consumerName Nome identificador do consumidor (ex: "CustomerContractConsumer")
     * @param action Ação a ser executada
     * @return true se foi executado agora, false se foi ignorado por duplicidade
     */
    public boolean executeIdempotent(@NonNull UUID eventId, @NonNull String consumerName, @NonNull Runnable action) {
        if (isAlreadyProcessed(eventId, consumerName)) {
            log.info("Evento já processado anteriormente, ignorando: eventId={}, consumer={}", eventId, consumerName);
            return false;
        }

        action.run();
        markAsProcessed(eventId, consumerName);
        return true;
    }
}
