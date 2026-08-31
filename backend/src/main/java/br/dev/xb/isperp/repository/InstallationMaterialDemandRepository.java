package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.InstallationMaterialDemand;
import br.dev.xb.isperp.entity.MaterialDemandStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InstallationMaterialDemandRepository extends JpaRepository<InstallationMaterialDemand, UUID> {

    Optional<InstallationMaterialDemand> findByWorkOrderId(UUID workOrderId);

    Optional<InstallationMaterialDemand> findByContractId(UUID contractId);

    List<InstallationMaterialDemand> findByStatus(MaterialDemandStatus status);

    List<InstallationMaterialDemand> findByAllocatedWarehouseId(UUID warehouseId);
}
