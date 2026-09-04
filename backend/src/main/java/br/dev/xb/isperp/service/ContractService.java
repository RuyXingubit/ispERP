package br.dev.xb.isperp.service;

import br.dev.xb.isperp.entity.Contract;
import br.dev.xb.isperp.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class ContractService {

    private final ContractRepository contractRepository;

    public List<Contract> getAllContracts() {
        return contractRepository.findAll();
    }

    public Optional<Contract> getContractById(UUID id) {
        return contractRepository.findById(id);
    }

    public List<Contract> getContractsByCustomerId(UUID customerId) {
        return contractRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    public List<Contract> getContractsByStatus(Contract.ContractStatus status) {
        return contractRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    public Contract createContract(Contract contract) {
        return contractRepository.save(contract);
    }

    public Contract updateStatus(UUID id, Contract.ContractStatus newStatus) {
        Contract contract = contractRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contrato não encontrado"));

        contract.setStatus(newStatus);
        return contractRepository.save(contract);
    }

    public Contract updateContract(Contract contract) {
        return contractRepository.save(contract);
    }
}
