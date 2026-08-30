package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.RadiusLifecycleLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RadiusLifecycleLogRepository extends JpaRepository<RadiusLifecycleLog, UUID> {

    List<RadiusLifecycleLog> findByContractIdOrderByCreatedAtDesc(UUID contractId);

    List<RadiusLifecycleLog> findByUsernameOrderByCreatedAtDesc(String username);

    Page<RadiusLifecycleLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
