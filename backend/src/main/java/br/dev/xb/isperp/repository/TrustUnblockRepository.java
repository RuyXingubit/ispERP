package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.TrustUnblock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TrustUnblockRepository extends JpaRepository<TrustUnblock, UUID> {

    List<TrustUnblock> findByContractIdOrderByRequestedAtDesc(UUID contractId);

    Optional<TrustUnblock> findFirstByContractIdAndStatusOrderByRequestedAtDesc(UUID contractId, String status);
}
