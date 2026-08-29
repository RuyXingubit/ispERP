package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.HelpdeskTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HelpdeskTicketRepository extends JpaRepository<HelpdeskTicket, UUID> {

    Optional<HelpdeskTicket> findByProtocol(String protocol);

    List<HelpdeskTicket> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    List<HelpdeskTicket> findByStatusOrderByCreatedAtDesc(HelpdeskTicket.TicketStatus status);

    List<HelpdeskTicket> findByAssignedToUserIdOrderByCreatedAtDesc(UUID assignedToUserId);

    boolean existsByProtocol(String protocol);
}
