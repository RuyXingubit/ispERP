package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.IpamSubnet;
import br.dev.xb.isperp.ipam.IpamIpVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IpamSubnetRepository extends JpaRepository<IpamSubnet, UUID> {
    List<IpamSubnet> findByParentIsNull();
    List<IpamSubnet> findByParentId(UUID parentId);
    List<IpamSubnet> findByVrfId(UUID vrfId);
    List<IpamSubnet> findByIpVersion(IpamIpVersion ipVersion);
    Optional<IpamSubnet> findByCidr(String cidr);
    Optional<IpamSubnet> findByIsPoolTrueAndPoolName(String poolName);

    @Query("SELECT s FROM IpamSubnet s LEFT JOIN FETCH s.vrf LEFT JOIN FETCH s.asn ORDER BY s.prefixLength ASC, s.cidr ASC")
    List<IpamSubnet> findAllWithRelations();
}
