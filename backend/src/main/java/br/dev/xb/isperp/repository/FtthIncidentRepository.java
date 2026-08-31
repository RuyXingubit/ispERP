package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.FtthIncident;
import br.dev.xb.isperp.monitoring.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FtthIncidentRepository extends JpaRepository<FtthIncident, UUID> {
    List<FtthIncident> findByStatusInOrderByDetectedAtDesc(List<IncidentStatus> statuses);
    List<FtthIncident> findAllByOrderByDetectedAtDesc();
    long countByStatus(IncidentStatus status);
}
