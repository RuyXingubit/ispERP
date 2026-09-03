package br.dev.xb.isperp.repository.financial;

import br.dev.xb.isperp.entity.financial.ExpenseInstallment;
import br.dev.xb.isperp.entity.financial.PayableStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExpenseInstallmentRepository extends JpaRepository<ExpenseInstallment, UUID> {
    List<ExpenseInstallment> findByPayableInvoiceIdOrderByInstallmentNumberAsc(UUID payableInvoiceId);
    List<ExpenseInstallment> findByDueDateBetweenOrderByDueDateAsc(LocalDate start, LocalDate end);
    List<ExpenseInstallment> findByStatusOrderByDueDateAsc(PayableStatus status);
}
