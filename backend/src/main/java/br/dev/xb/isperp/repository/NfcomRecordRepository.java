package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.NfcomRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NfcomRecordRepository extends JpaRepository<NfcomRecord, UUID> {
    Optional<NfcomRecord> findByChaveAcesso(String chaveAcesso);
    Optional<NfcomRecord> findByInvoiceId(UUID invoiceId);
    Page<NfcomRecord> findByCompanyId(UUID companyId, Pageable pageable);
    List<NfcomRecord> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    List<NfcomRecord> findByCompanyIdAndCreatedAtBetween(UUID companyId, LocalDateTime start, LocalDateTime end);
}
