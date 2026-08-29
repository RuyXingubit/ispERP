package br.dev.xb.isperp.service;

import br.dev.xb.isperp.entity.Contract;
import br.dev.xb.isperp.repository.ContractRepository;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class ContractServiceTest {

    @Mock
    private ContractRepository contractRepository;

    @InjectMocks
    private ContractService contractService;

    private Contract sampleContract;
    private UUID contractId;

    @BeforeEach
    void setUp() {
        contractId = UuidCreatorUtils.generateUuidV7();
        sampleContract = Contract.builder()
                .id(contractId)
                .customerId(UuidCreatorUtils.generateUuidV7())
                .planId(UuidCreatorUtils.generateUuidV7())
                .contractNumber("CTR-202608001")
                .status(Contract.ContractStatus.PENDING_INSTALLATION)
                .monthlyFee(new BigDecimal("99.90"))
                .dueDay(10)
                .installationAddress("Rua das Flores, 123")
                .build();
    }

    @Test
    @DisplayName("Deve criar contrato com status PENDING_INSTALLATION")
    void shouldCreateContract() {
        when(contractRepository.save(any(Contract.class))).thenAnswer(i -> i.getArgument(0));

        Contract created = contractService.createContract(sampleContract);

        assertNotNull(created);
        assertEquals(Contract.ContractStatus.PENDING_INSTALLATION, created.getStatus());
        verify(contractRepository, times(1)).save(sampleContract);
    }

    @Test
    @DisplayName("Deve atualizar status do contrato para ACTIVE")
    void shouldUpdateContractStatus() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(sampleContract));
        when(contractRepository.save(any(Contract.class))).thenAnswer(i -> i.getArgument(0));

        Contract updated = contractService.updateStatus(contractId, Contract.ContractStatus.ACTIVE);

        assertEquals(Contract.ContractStatus.ACTIVE, updated.getStatus());
        verify(contractRepository, times(1)).save(sampleContract);
    }
}
