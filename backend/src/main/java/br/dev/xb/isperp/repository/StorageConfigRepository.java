package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.StorageConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StorageConfigRepository extends JpaRepository<StorageConfig, UUID> {

    Optional<StorageConfig> findFirstByIsActiveTrueOrderByCreatedAtDesc();

    Optional<StorageConfig> findFirstByCompanyIdAndIsActiveTrue(UUID companyId);
}
