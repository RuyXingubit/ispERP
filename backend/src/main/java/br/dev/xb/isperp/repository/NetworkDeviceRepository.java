package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.NetworkDevice;
import br.dev.xb.isperp.network.NetworkDriverType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NetworkDeviceRepository extends JpaRepository<NetworkDevice, UUID> {

    List<NetworkDevice> findByActiveTrue();

    Optional<NetworkDevice> findFirstByDriverTypeAndActiveTrue(NetworkDriverType driverType);

    Optional<NetworkDevice> findFirstByActiveTrueOrderByCreatedAtAsc();
}
