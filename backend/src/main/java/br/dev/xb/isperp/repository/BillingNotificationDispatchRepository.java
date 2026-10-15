package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.BillingNotificationDispatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BillingNotificationDispatchRepository extends JpaRepository<BillingNotificationDispatch, UUID> {

    boolean existsByInvoiceIdAndStageAndChannel(
            UUID invoiceId,
            BillingNotificationDispatch.Stage stage,
            String channel
    );

    Optional<BillingNotificationDispatch> findByInvoiceIdAndStageAndChannel(
            UUID invoiceId,
            BillingNotificationDispatch.Stage stage,
            String channel
    );

    List<BillingNotificationDispatch> findAllByOrderByDispatchedAtDesc();

    List<BillingNotificationDispatch> findByInvoiceIdOrderByDispatchedAtDesc(UUID invoiceId);
}
