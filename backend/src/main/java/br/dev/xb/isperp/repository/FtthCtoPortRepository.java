package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.FtthCtoPort;
import br.dev.xb.isperp.ftth.FtthPortStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FtthCtoPortRepository extends JpaRepository<FtthCtoPort, UUID> {
    List<FtthCtoPort> findByCtoIdOrderByPortNumberAsc(UUID ctoId);
    Optional<FtthCtoPort> findByCtoIdAndPortNumber(UUID ctoId, int portNumber);
    Optional<FtthCtoPort> findByOnuProvisioningId(UUID onuProvisioningId);
    long countByCtoIdAndStatus(UUID ctoId, FtthPortStatus status);
}
