package br.dev.xb.isperp.repository.financial;

import br.dev.xb.isperp.entity.financial.BankDepositConfirmation;
import br.dev.xb.isperp.entity.financial.BankDepositStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BankDepositConfirmationRepository extends JpaRepository<BankDepositConfirmation, UUID> {
    List<BankDepositConfirmation> findByDepositorIdOrderByDepositDateDesc(UUID depositorId);
    List<BankDepositConfirmation> findByStatusOrderByDepositDateDesc(BankDepositStatus status);
}
