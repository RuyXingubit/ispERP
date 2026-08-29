package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    List<Invoice> findByCustomerIdOrderByDueDateDesc(UUID customerId);

    List<Invoice> findByContractIdOrderByDueDateDesc(UUID contractId);

    List<Invoice> findByStatusOrderByDueDateAsc(Invoice.InvoiceStatus status);

    Optional<Invoice> findByExternalTransactionId(String externalTransactionId);

    boolean existsByContractIdAndDueDate(UUID contractId, LocalDate dueDate);
}
