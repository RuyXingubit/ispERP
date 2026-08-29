package com.xingubit.isperp.repository;

import com.xingubit.isperp.entity.SiteSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SiteSettingsRepository extends JpaRepository<SiteSettings, UUID> {
    
    Optional<SiteSettings> findFirstByOrderByCreatedAtAsc();
}