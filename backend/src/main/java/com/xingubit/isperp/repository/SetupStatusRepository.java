package com.xingubit.isperp.repository;

import com.xingubit.isperp.entity.SetupStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SetupStatusRepository extends JpaRepository<SetupStatus, Long> {
    
    Optional<SetupStatus> findFirstByOrderByIdAsc();
}