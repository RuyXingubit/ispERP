package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.TicketInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TicketInteractionRepository extends JpaRepository<TicketInteraction, UUID> {

    List<TicketInteraction> findByTicketIdOrderByCreatedAtAsc(UUID ticketId);

    List<TicketInteraction> findByTicketIdAndIsInternalNoteFalseOrderByCreatedAtAsc(UUID ticketId);
}
