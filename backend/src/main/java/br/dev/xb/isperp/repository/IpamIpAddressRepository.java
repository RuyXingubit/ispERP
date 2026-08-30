package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.IpamIpAddress;
import br.dev.xb.isperp.ipam.IpamAssignedToType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IpamIpAddressRepository extends JpaRepository<IpamIpAddress, UUID> {
    List<IpamIpAddress> findBySubnetId(UUID subnetId);
    Optional<IpamIpAddress> findBySubnetIdAndIpAddress(UUID subnetId, String ipAddress);
    Optional<IpamIpAddress> findByIpAddress(String ipAddress);
    List<IpamIpAddress> findByAssignedToTypeAndAssignedToId(IpamAssignedToType assignedToType, UUID assignedToId);

    @Query("SELECT COUNT(i) FROM IpamIpAddress i WHERE i.subnet.id = :subnetId AND i.status <> 'AVAILABLE'")
    long countAllocatedBySubnetId(@Param("subnetId") UUID subnetId);

    @Query("SELECT i.ipAddress FROM IpamIpAddress i WHERE i.subnet.id = :subnetId")
    List<String> findUsedIpsBySubnetId(@Param("subnetId") UUID subnetId);
}
