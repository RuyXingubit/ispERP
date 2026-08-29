package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.NotificationConfig;
import br.dev.xb.isperp.notification.whatsapp.WhatsAppProviderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationConfigRepository extends JpaRepository<NotificationConfig, UUID> {

    List<NotificationConfig> findByActiveTrue();

    Optional<NotificationConfig> findFirstByProviderTypeAndActiveTrue(WhatsAppProviderType providerType);

    Optional<NotificationConfig> findFirstByActiveTrueOrderByCreatedAtAsc();
}
