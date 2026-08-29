package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.FiscalGatewayConfig;
import br.dev.xb.isperp.fiscal.FiscalGatewayType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FiscalGatewayConfigRepository extends JpaRepository<FiscalGatewayConfig, UUID> {
    Optional<FiscalGatewayConfig> findByCompanyIdAndIsActiveTrue(UUID companyId);
    List<FiscalGatewayConfig> findByCompanyId(UUID companyId);
    Optional<FiscalGatewayConfig> findByCompanyIdAndGatewayType(UUID companyId, FiscalGatewayType gatewayType);
    Optional<FiscalGatewayConfig> findFirstByIsActiveTrue();
}
