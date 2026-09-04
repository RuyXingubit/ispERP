package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.contract.ContractsApi;
import br.dev.xb.isperp.api.dto.ContractCreateRequest;
import br.dev.xb.isperp.api.dto.ContractResponse;
import br.dev.xb.isperp.api.dto.ContractStatus;
import br.dev.xb.isperp.api.dto.ContractUpdateRequest;
import br.dev.xb.isperp.api.dto.UpdateContractStatusRequest;
import br.dev.xb.isperp.entity.Contract;
import br.dev.xb.isperp.mapper.ContractMapper;
import br.dev.xb.isperp.service.ContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@SuppressWarnings("null")
public class ContractController implements ContractsApi {

    private final ContractService contractService;
    private final ContractMapper contractMapper;

    @Override
    public ResponseEntity<ContractResponse> createContract(ContractCreateRequest contractCreateRequest) {
        Contract entity = contractMapper.toEntity(contractCreateRequest);
        Contract created = contractService.createContract(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(contractMapper.toResponse(created));
    }

    @Override
    public ResponseEntity<List<ContractResponse>> getAllContracts() {
        List<Contract> contracts = contractService.getAllContracts();
        return ResponseEntity.ok(contractMapper.toResponseList(contracts));
    }

    @Override
    public ResponseEntity<ContractResponse> getContractById(UUID id) {
        Optional<Contract> contract = contractService.getContractById(id);
        return contract.map(c -> ResponseEntity.ok(contractMapper.toResponse(c)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<List<ContractResponse>> getContractsByCustomerId(UUID customerId) {
        List<Contract> contracts = contractService.getContractsByCustomerId(customerId);
        return ResponseEntity.ok(contractMapper.toResponseList(contracts));
    }

    @Override
    public ResponseEntity<List<ContractResponse>> getContractsByStatus(ContractStatus status) {
        Contract.ContractStatus entityStatus = contractMapper.toEntityStatus(status);
        List<Contract> contracts = contractService.getContractsByStatus(entityStatus);
        return ResponseEntity.ok(contractMapper.toResponseList(contracts));
    }

    @Override
    public ResponseEntity<ContractResponse> updateContract(UUID id, ContractUpdateRequest contractUpdateRequest) {
        Optional<Contract> contractOpt = contractService.getContractById(id);
        if (contractOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Contract contract = contractOpt.get();
        contractMapper.updateEntityFromRequest(contractUpdateRequest, contract);
        Contract updated = contractService.updateContract(contract);
        return ResponseEntity.ok(contractMapper.toResponse(updated));
    }

    @Override
    public ResponseEntity<ContractResponse> updateContractStatus(
            UUID id,
            ContractStatus status,
            UpdateContractStatusRequest updateContractStatusRequest) {

        ContractStatus targetStatus = status;
        if (targetStatus == null && updateContractStatusRequest != null) {
            targetStatus = updateContractStatusRequest.getStatus();
        }

        if (targetStatus == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            Contract.ContractStatus entityStatus = contractMapper.toEntityStatus(targetStatus);
            Contract updated = contractService.updateStatus(id, entityStatus);
            return ResponseEntity.ok(contractMapper.toResponse(updated));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
