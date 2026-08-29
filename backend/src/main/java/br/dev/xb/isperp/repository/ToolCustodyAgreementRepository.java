package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.ToolCustodyAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ToolCustodyAgreementRepository extends JpaRepository<ToolCustodyAgreement, UUID> {

    Optional<ToolCustodyAgreement> findByCode(String code);

    List<ToolCustodyAgreement> findByStatus(ToolCustodyAgreement.AgreementStatus status);

    List<ToolCustodyAgreement> findByHolderUserIdAndStatus(UUID holderUserId, ToolCustodyAgreement.AgreementStatus status);
}
