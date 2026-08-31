package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.OltPonPort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OltPonPortRepository extends JpaRepository<OltPonPort, UUID> {
    List<OltPonPort> findByNetworkDeviceIdOrderBySlotNumberAscPortNumberAsc(UUID networkDeviceId);
    Optional<OltPonPort> findByNetworkDeviceIdAndSlotNumberAndPortNumber(UUID networkDeviceId, int slotNumber, int portNumber);
    long countByOperStatus(String operStatus);
}
