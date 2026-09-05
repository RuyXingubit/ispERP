package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.LegalCollectionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LegalCollectionRepository extends JpaRepository<LegalCollectionRecord, UUID> {

    List<LegalCollectionRecord> findByCustomerId(UUID customerId);

    List<LegalCollectionRecord> findByContractId(UUID contractId);

    Optional<LegalCollectionRecord> findByWorkOrderId(UUID workOrderId);

    List<LegalCollectionRecord> findByStatus(LegalCollectionRecord.LegalCollectionStatus status);
}
