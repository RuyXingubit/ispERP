package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.FiscalRegimeTransition;
import br.dev.xb.isperp.fiscal.FiscalRegimeTransitionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface FiscalRegimeTransitionRepository extends JpaRepository<FiscalRegimeTransition, UUID> {

    List<FiscalRegimeTransition> findByCompanyIdOrderByEffectiveDateDescCreatedAtDesc(UUID companyId);

    @Query("SELECT t FROM FiscalRegimeTransition t WHERE t.status = :status AND t.effectiveDate <= :date ORDER BY t.effectiveDate ASC")
    List<FiscalRegimeTransition> findPendingTransitionsToApply(
            @Param("status") FiscalRegimeTransitionStatus status,
            @Param("date") LocalDate date
    );
}
