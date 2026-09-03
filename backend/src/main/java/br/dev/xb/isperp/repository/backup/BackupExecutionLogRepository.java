package br.dev.xb.isperp.repository.backup;

import br.dev.xb.isperp.backup.BackupStatus;
import br.dev.xb.isperp.entity.backup.BackupExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BackupExecutionLogRepository extends JpaRepository<BackupExecutionLog, UUID> {

    List<BackupExecutionLog> findAllByOrderByStartedAtDesc();

    Optional<BackupExecutionLog> findFirstByStatusOrderByCompletedAtDesc(BackupStatus status);

    Optional<BackupExecutionLog> findFirstByIsDryRunVerifiedTrueOrderByDryRunVerifiedAtDesc();

    List<BackupExecutionLog> findByStatusAndStartedAtBefore(BackupStatus status, OffsetDateTime cutoff);
}
