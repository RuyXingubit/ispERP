package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.RadiusPolicyConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RadiusPolicyConfigRepository extends JpaRepository<RadiusPolicyConfig, UUID> {

    @Query("SELECT c FROM RadiusPolicyConfig c ORDER BY c.createdAt ASC LIMIT 1")
    Optional<RadiusPolicyConfig> findFirstConfig();
}
