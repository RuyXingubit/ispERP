package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContractRepository extends JpaRepository<Contract, UUID> {

    Optional<Contract> findByContractNumber(String contractNumber);

    List<Contract> findByCustomerId(UUID customerId);

    List<Contract> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    List<Contract> findByStatusOrderByCreatedAtDesc(Contract.ContractStatus status);

    long countByStatus(Contract.ContractStatus status);
}
