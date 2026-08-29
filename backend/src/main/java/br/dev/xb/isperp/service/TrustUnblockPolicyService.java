package br.dev.xb.isperp.service;

import br.dev.xb.isperp.entity.Contract;
import br.dev.xb.isperp.entity.TrustUnblock;
import br.dev.xb.isperp.event.GenericDomainEvent;
import br.dev.xb.isperp.repository.ContractRepository;
import br.dev.xb.isperp.repository.TrustUnblockRepository;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrustUnblockPolicyService {

    private final TrustUnblockRepository trustUnblockRepository;
    private final ContractRepository contractRepository;
    private final DomainEventPublisher eventPublisher;

    @Data
    @Builder
    public static class UnblockEvaluationResult {
        private boolean granted;
        private String message;
        private String unblockType;
        private LocalDateTime expiresAt;
    }

    /**
     * Solicitação de Desbloqueio Temporário pelo Bot do WhatsApp (Autoatendimento).
     */
    @Transactional
    public UnblockEvaluationResult requestBotAutoUnblock(UUID contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contrato não encontrado"));

        List<TrustUnblock> history = trustUnblockRepository.findByContractIdOrderByRequestedAtDesc(contractId);

        // Verifica se já usou a liberação automática do Bot
        boolean alreadyUsedBot = history.stream()
                .anyMatch(u -> "BOT_AUTO".equalsIgnoreCase(u.getUnblockType()) && u.getRequestedAt().isAfter(LocalDateTime.now().minusDays(30)));

        if (alreadyUsedBot) {
            log.warn("Contrato {} já utilizou a liberação automática do Bot neste ciclo.", contract.getContractNumber());
            return UnblockEvaluationResult.builder()
                    .granted(false)
                    .message("Você já utilizou sua liberação temporária de 24h automática para este período. Por favor, regularize via Pix ou solicite a um atendente.")
                    .build();
        }

        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);
        TrustUnblock unblock = TrustUnblock.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .contractId(contractId)
                .requestedAt(LocalDateTime.now())
                .expiresAt(expiresAt)
                .unblockType("BOT_AUTO")
                .status("ACTIVE")
                .build();
        trustUnblockRepository.save(unblock);

        contract.setStatus(Contract.ContractStatus.ACTIVE);
        contractRepository.save(contract);

        Map<String, Object> payload = new HashMap<>();
        payload.put("contractId", contractId.toString());
        payload.put("customerId", contract.getCustomerId().toString());
        payload.put("unblockType", "BOT_AUTO");
        payload.put("expiresAt", expiresAt.toString());

        GenericDomainEvent event = GenericDomainEvent.builder()
                .eventId(UuidCreatorUtils.generateUuidV7())
                .eventType("CONTRACT_ACCESS_RESTORE_REQUESTED")
                .aggregateType("Contract")
                .aggregateId(contractId.toString())
                .payload(payload)
                .build();

        eventPublisher.publish(event);

        log.info("Desbloqueio temporário de 24h (Bot) concedido com sucesso para o contrato {}", contract.getContractNumber());
        return UnblockEvaluationResult.builder()
                .granted(true)
                .unblockType("BOT_AUTO")
                .expiresAt(expiresAt)
                .message("Sua conexão foi liberada temporariamente por 24 horas! Ela voltará automaticamente em poucos instantes.")
                .build();
    }

    /**
     * Solicitação de Desbloqueio Temporário pelo Atendente de Suporte no ERP (Exclusivo 2ª Tentativa).
     */
    @Transactional
    public UnblockEvaluationResult requestAttendantManualUnblock(UUID contractId, UUID attendantUserId, String reason) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contrato não encontrado"));

        List<TrustUnblock> history = trustUnblockRepository.findByContractIdOrderByRequestedAtDesc(contractId);

        // Verifica se o atendente já concedeu a 2ª liberação
        boolean alreadyUsedAttendant = history.stream()
                .anyMatch(u -> "ATTENDANT_MANUAL".equalsIgnoreCase(u.getUnblockType()) && u.getRequestedAt().isAfter(LocalDateTime.now().minusDays(30)));

        if (alreadyUsedAttendant) {
            log.warn("Contrato {} já utilizou a liberação excepcional de atendente. Trava total ativada.", contract.getContractNumber());
            return UnblockEvaluationResult.builder()
                    .granted(false)
                    .message("Limite máximo de liberações temporárias atingido para este contrato. Nova liberação permitida apenas mediante liquidação via Pix.")
                    .build();
        }

        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);
        TrustUnblock unblock = TrustUnblock.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .contractId(contractId)
                .requestedAt(LocalDateTime.now())
                .expiresAt(expiresAt)
                .unblockType("ATTENDANT_MANUAL")
                .grantedByUserId(attendantUserId)
                .status("ACTIVE")
                .build();
        trustUnblockRepository.save(unblock);

        contract.setStatus(Contract.ContractStatus.ACTIVE);
        contractRepository.save(contract);

        Map<String, Object> payload = new HashMap<>();
        payload.put("contractId", contractId.toString());
        payload.put("customerId", contract.getCustomerId().toString());
        payload.put("unblockType", "ATTENDANT_MANUAL");
        payload.put("grantedByUserId", attendantUserId != null ? attendantUserId.toString() : null);
        payload.put("reason", reason);
        payload.put("expiresAt", expiresAt.toString());

        GenericDomainEvent event = GenericDomainEvent.builder()
                .eventId(UuidCreatorUtils.generateUuidV7())
                .eventType("CONTRACT_ACCESS_RESTORE_REQUESTED")
                .aggregateType("Contract")
                .aggregateId(contractId.toString())
                .payload(payload)
                .build();

        eventPublisher.publish(event);

        log.info("Desbloqueio temporário excepcional (Atendente {}) concedido para o contrato {}", attendantUserId, contract.getContractNumber());
        return UnblockEvaluationResult.builder()
                .granted(true)
                .unblockType("ATTENDANT_MANUAL")
                .expiresAt(expiresAt)
                .message("Desbloqueio de 24h concedido pelo atendente com sucesso.")
                .build();
    }
}
