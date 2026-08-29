package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.OutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxEvent.OutboxStatus status, Pageable pageable);

    List<OutboxEvent> findByStatusInOrderByCreatedAtAsc(List<OutboxEvent.OutboxStatus> statuses, Pageable pageable);

    long countByStatus(OutboxEvent.OutboxStatus status);
}
