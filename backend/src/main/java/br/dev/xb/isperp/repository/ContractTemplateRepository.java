package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.ContractTemplate;
import br.dev.xb.isperp.signature.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContractTemplateRepository extends JpaRepository<ContractTemplate, UUID> {

    List<ContractTemplate> findByCompanyId(UUID companyId);

    List<ContractTemplate> findByDocumentType(DocumentType documentType);

    Optional<ContractTemplate> findFirstByCompanyIdAndDocumentTypeAndIsActiveTrueOrderByVersionDesc(UUID companyId, DocumentType documentType);

    Optional<ContractTemplate> findFirstByDocumentTypeAndIsActiveTrueOrderByVersionDesc(DocumentType documentType);
}
