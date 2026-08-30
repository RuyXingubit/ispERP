package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.MarcoCivilReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MarcoCivilReportRepository extends JpaRepository<MarcoCivilReport, UUID> {
    Optional<MarcoCivilReport> findByValidationToken(String validationToken);
    Optional<MarcoCivilReport> findBySha256Hash(String sha256Hash);
}
