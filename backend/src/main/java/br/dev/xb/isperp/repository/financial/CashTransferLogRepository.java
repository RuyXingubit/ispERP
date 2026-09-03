package br.dev.xb.isperp.repository.financial;

import br.dev.xb.isperp.entity.financial.CashTransferLog;
import br.dev.xb.isperp.entity.financial.CashTransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CashTransferLogRepository extends JpaRepository<CashTransferLog, UUID> {
    List<CashTransferLog> findBySenderIdOrderByRequestedAtDesc(UUID senderId);
    List<CashTransferLog> findByReceiverIdOrderByRequestedAtDesc(UUID receiverId);
    List<CashTransferLog> findByReceiverIdAndStatus(UUID receiverId, CashTransferStatus status);
}
