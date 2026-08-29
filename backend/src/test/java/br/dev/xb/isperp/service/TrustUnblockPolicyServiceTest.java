package br.dev.xb.isperp.service;

import br.dev.xb.isperp.entity.Contract;
import br.dev.xb.isperp.entity.TrustUnblock;
import br.dev.xb.isperp.repository.ContractRepository;
import br.dev.xb.isperp.repository.TrustUnblockRepository;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrustUnblockPolicyServiceTest {

    @Mock
    private TrustUnblockRepository trustUnblockRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private DomainEventPublisher eventPublisher;

    @InjectMocks
    private TrustUnblockPolicyService trustUnblockPolicyService;

    private UUID contractId;
    private UUID customerId;
    private Contract contract;

    @BeforeEach
    void setUp() {
        contractId = UuidCreatorUtils.generateUuidV7();
        customerId = UuidCreatorUtils.generateUuidV7();
        contract = Contract.builder()
                .id(contractId)
                .customerId(customerId)
                .contractNumber("CTR-001")
                .status(Contract.ContractStatus.SUSPENDED)
                .build();
    }

    @Test
    @DisplayName("Deve conceder 24h de desbloqueio automático no primeiro pedido do Bot")
    void shouldGrant24hAutoUnblockOnFirstBotRequest() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(trustUnblockRepository.findByContractIdOrderByRequestedAtDesc(contractId)).thenReturn(new ArrayList<>());
        when(trustUnblockRepository.save(any(TrustUnblock.class))).thenAnswer(i -> i.getArgument(0));

        TrustUnblockPolicyService.UnblockEvaluationResult result = trustUnblockPolicyService.requestBotAutoUnblock(contractId);

        assertTrue(result.isGranted());
        assertEquals("BOT_AUTO", result.getUnblockType());
        assertNotNull(result.getExpiresAt());
        assertEquals(Contract.ContractStatus.ACTIVE, contract.getStatus());
        verify(eventPublisher, times(1)).publish(any(br.dev.xb.isperp.event.DomainEvent.class));
    }

    @Test
    @DisplayName("Deve negar pedido de desbloqueio do Bot se ele já tiver sido utilizado no período")
    void shouldRejectSecondBotRequest() {
        TrustUnblock previousUnblock = TrustUnblock.builder()
                .contractId(contractId)
                .unblockType("BOT_AUTO")
                .requestedAt(LocalDateTime.now().minusDays(2))
                .build();

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(trustUnblockRepository.findByContractIdOrderByRequestedAtDesc(contractId)).thenReturn(List.of(previousUnblock));

        TrustUnblockPolicyService.UnblockEvaluationResult result = trustUnblockPolicyService.requestBotAutoUnblock(contractId);

        assertFalse(result.isGranted());
        assertTrue(result.getMessage().contains("já utilizou sua liberação temporária de 24h automática"));
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("Deve permitir 2ª liberação excepcional realizada por atendente humano")
    void shouldAllowSecondUnblockWhenGrantedByAttendant() {
        TrustUnblock botUnblock = TrustUnblock.builder()
                .contractId(contractId)
                .unblockType("BOT_AUTO")
                .requestedAt(LocalDateTime.now().minusDays(2))
                .build();

        UUID attendantId = UuidCreatorUtils.generateUuidV7();

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(trustUnblockRepository.findByContractIdOrderByRequestedAtDesc(contractId)).thenReturn(List.of(botUnblock));
        when(trustUnblockRepository.save(any(TrustUnblock.class))).thenAnswer(i -> i.getArgument(0));

        TrustUnblockPolicyService.UnblockEvaluationResult result = trustUnblockPolicyService.requestAttendantManualUnblock(
                contractId, attendantId, "Cliente solicitou prorrogação para pagar no fim da tarde"
        );

        assertTrue(result.isGranted());
        assertEquals("ATTENDANT_MANUAL", result.getUnblockType());
        verify(eventPublisher, times(1)).publish(any(br.dev.xb.isperp.event.DomainEvent.class));
    }

    @Test
    @DisplayName("Deve travar completamente no 3º pedido de desbloqueio exigindo pagamento Pix")
    void shouldLockPermanentlyOnThirdUnblockAttempt() {
        TrustUnblock botUnblock = TrustUnblock.builder()
                .contractId(contractId)
                .unblockType("BOT_AUTO")
                .requestedAt(LocalDateTime.now().minusDays(5))
                .build();

        TrustUnblock attendantUnblock = TrustUnblock.builder()
                .contractId(contractId)
                .unblockType("ATTENDANT_MANUAL")
                .requestedAt(LocalDateTime.now().minusDays(2))
                .build();

        UUID attendantId = UuidCreatorUtils.generateUuidV7();

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(trustUnblockRepository.findByContractIdOrderByRequestedAtDesc(contractId)).thenReturn(List.of(attendantUnblock, botUnblock));

        TrustUnblockPolicyService.UnblockEvaluationResult result = trustUnblockPolicyService.requestAttendantManualUnblock(
                contractId, attendantId, "Tentativa de 3ª liberação"
        );

        assertFalse(result.isGranted());
        assertTrue(result.getMessage().contains("Limite máximo de liberações temporárias atingido"));
        verify(eventPublisher, never()).publish(any());
    }
}
