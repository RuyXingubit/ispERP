package br.dev.xb.isperp.repository.backup;

import br.dev.xb.isperp.entity.backup.BackupDestination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BackupDestinationRepository extends JpaRepository<BackupDestination, UUID> {

    List<BackupDestination> findByIsActiveTrue();

    Optional<BackupDestination> findFirstByIsActiveTrueAndIsPrimaryTrue();
}
