package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.CustodyLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CustodyLogRepository extends JpaRepository<CustodyLog, UUID> {

    List<CustodyLog> findByAssetIdOrderByCreatedAtDesc(UUID assetId);

    List<CustodyLog> findByToUserIdOrderByCreatedAtDesc(UUID toUserId);

    List<CustodyLog> findByWorkOrderIdOrderByCreatedAtDesc(UUID workOrderId);
}
