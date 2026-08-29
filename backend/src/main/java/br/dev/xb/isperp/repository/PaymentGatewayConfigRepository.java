package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.PaymentGatewayConfig;
import br.dev.xb.isperp.gateway.PaymentGatewayType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentGatewayConfigRepository extends JpaRepository<PaymentGatewayConfig, UUID> {

    List<PaymentGatewayConfig> findByActiveTrue();

    Optional<PaymentGatewayConfig> findFirstByGatewayTypeAndActiveTrue(PaymentGatewayType gatewayType);

    Optional<PaymentGatewayConfig> findFirstByActiveTrueOrderByCreatedAtAsc();
}
