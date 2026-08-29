package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.FiscalCompany;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FiscalCompanyRepository extends JpaRepository<FiscalCompany, UUID> {
    Optional<FiscalCompany> findByCnpj(String cnpj);
    Optional<FiscalCompany> findFirstByIsActiveTrue();
}
