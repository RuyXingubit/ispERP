package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.OnuTelemetryRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OnuTelemetryRecordRepository extends JpaRepository<OnuTelemetryRecord, UUID> {
    List<OnuTelemetryRecord> findByOnuProvisioningIdOrderByRecordedAtDesc(UUID onuProvisioningId);
}
