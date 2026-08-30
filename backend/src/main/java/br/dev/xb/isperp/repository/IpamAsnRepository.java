package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.IpamAsn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IpamAsnRepository extends JpaRepository<IpamAsn, UUID> {
    Optional<IpamAsn> findByAsn(Long asn);
}
