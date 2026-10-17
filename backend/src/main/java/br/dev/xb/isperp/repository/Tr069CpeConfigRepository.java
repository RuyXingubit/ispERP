package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.Tr069CpeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface Tr069CpeConfigRepository extends JpaRepository<Tr069CpeConfig, UUID> {
    Optional<Tr069CpeConfig> findByContractId(UUID contractId);
}
