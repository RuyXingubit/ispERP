package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

    List<PaymentTransaction> findByInvoiceIdOrderByCreatedAtDesc(UUID invoiceId);
}
