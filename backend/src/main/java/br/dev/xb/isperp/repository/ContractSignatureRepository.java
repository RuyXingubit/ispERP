package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.ContractSignature;
import br.dev.xb.isperp.signature.SignatureStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContractSignatureRepository extends JpaRepository<ContractSignature, UUID> {

    Optional<ContractSignature> findByToken(String token);

    Optional<ContractSignature> findByPixTxid(String pixTxid);

    List<ContractSignature> findByContractIdOrderByCreatedAtDesc(UUID contractId);

    List<ContractSignature> findByStatus(SignatureStatus status);
}
