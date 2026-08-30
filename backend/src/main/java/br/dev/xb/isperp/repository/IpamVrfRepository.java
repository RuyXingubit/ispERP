package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.IpamVrf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IpamVrfRepository extends JpaRepository<IpamVrf, UUID> {
    Optional<IpamVrf> findByName(String name);
    Optional<IpamVrf> findByIsDefaultTrue();
}
