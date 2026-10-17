package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.Tr069CommandLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface Tr069CommandLogRepository extends JpaRepository<Tr069CommandLog, UUID> {
    List<Tr069CommandLog> findTop15ByContractIdOrderByCreatedAtDesc(UUID contractId);
}
