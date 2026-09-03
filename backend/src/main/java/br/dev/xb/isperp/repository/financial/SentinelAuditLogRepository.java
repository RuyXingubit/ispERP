package br.dev.xb.isperp.repository.financial;

import br.dev.xb.isperp.entity.financial.SentinelAuditLog;
import br.dev.xb.isperp.entity.financial.SentinelSeverity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SentinelAuditLogRepository extends JpaRepository<SentinelAuditLog, UUID> {
    List<SentinelAuditLog> findByResolvedFalseOrderByCreatedAtDesc();
    List<SentinelAuditLog> findBySeverityOrderByCreatedAtDesc(SentinelSeverity severity);
}
