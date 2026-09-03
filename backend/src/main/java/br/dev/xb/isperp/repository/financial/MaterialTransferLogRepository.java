package br.dev.xb.isperp.repository.financial;

import br.dev.xb.isperp.entity.financial.CashTransferStatus;
import br.dev.xb.isperp.entity.financial.MaterialTransferLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MaterialTransferLogRepository extends JpaRepository<MaterialTransferLog, UUID> {
    List<MaterialTransferLog> findBySenderIdOrderByRequestedAtDesc(UUID senderId);
    List<MaterialTransferLog> findByReceiverIdOrderByRequestedAtDesc(UUID receiverId);
    List<MaterialTransferLog> findByReceiverIdAndStatus(UUID receiverId, CashTransferStatus status);
}
