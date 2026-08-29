package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.StockTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockTransferRepository extends JpaRepository<StockTransfer, UUID> {

    Optional<StockTransfer> findByCode(String code);

    List<StockTransfer> findByStatusOrderByCreatedAtDesc(StockTransfer.TransferStatus status);

    List<StockTransfer> findByCarrierUserIdAndStatus(UUID carrierUserId, StockTransfer.TransferStatus status);
}
