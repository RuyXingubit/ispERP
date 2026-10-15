package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.ContractLifecycleEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContractLifecycleEventRepository extends JpaRepository<ContractLifecycleEvent, UUID> {
    List<ContractLifecycleEvent> findByContractIdOrderByCreatedAtDesc(UUID contractId);
}
