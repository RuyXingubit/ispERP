package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.SerializedAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SerializedAssetRepository extends JpaRepository<SerializedAsset, UUID> {

    Optional<SerializedAsset> findBySerialNumber(String serialNumber);

    Optional<SerializedAsset> findByMacAddress(String macAddress);

    List<SerializedAsset> findByCurrentWarehouseIdAndStatus(UUID warehouseId, SerializedAsset.AssetStatus status);

    List<SerializedAsset> findByCurrentHolderUserIdAndStatus(UUID holderUserId, SerializedAsset.AssetStatus status);

    List<SerializedAsset> findByCurrentCustomerId(UUID customerId);

    List<SerializedAsset> findByStatus(SerializedAsset.AssetStatus status);
}
