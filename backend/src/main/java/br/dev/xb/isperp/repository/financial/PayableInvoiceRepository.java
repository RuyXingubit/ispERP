package br.dev.xb.isperp.repository.financial;

import br.dev.xb.isperp.entity.financial.PayableInvoice;
import br.dev.xb.isperp.entity.financial.PayableStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PayableInvoiceRepository extends JpaRepository<PayableInvoice, UUID> {
    List<PayableInvoice> findByStatusOrderByIssueDateDesc(PayableStatus status);
    List<PayableInvoice> findByChartOfAccountId(UUID chartOfAccountId);
}
