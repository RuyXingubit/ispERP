package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, ProcessedEvent.ProcessedEventId> {

    boolean existsByEventIdAndConsumerName(UUID eventId, String consumerName);
}
