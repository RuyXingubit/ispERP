package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.OnuProvisioning;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OnuProvisioningRepository extends JpaRepository<OnuProvisioning, UUID> {

    Optional<OnuProvisioning> findByContractId(UUID contractId);

    Optional<OnuProvisioning> findByOnuMac(String onuMac);

    Optional<OnuProvisioning> findByPppoeUser(String pppoeUser);

    List<OnuProvisioning> findByCustomerId(UUID customerId);

    List<OnuProvisioning> findByStatus(OnuProvisioning.OnuStatus status);
}
