package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.*;
import br.dev.xb.isperp.entity.*;
import br.dev.xb.isperp.mapper.RadiusLifecycleMapper;
import br.dev.xb.isperp.radius.NasVendorType;
import br.dev.xb.isperp.radius.RadiusLifecycleActionType;
import br.dev.xb.isperp.repository.*;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class RadiusLifecycleService {

    private final ContractRepository contractRepository;
    private final CustomerRepository customerRepository;
    private final PlanRepository planRepository;
    private final OnuProvisioningRepository onuProvisioningRepository;
    private final InvoiceRepository invoiceRepository;
    private final TrustUnblockRepository trustUnblockRepository;
    private final RadiusPolicyConfigRepository policyConfigRepository;
    private final RadiusLifecycleLogRepository lifecycleLogRepository;
    private final RadiusProvisioningService radiusProvisioningService;
    private final RadiusSessionService radiusSessionService;
    private final NasRepository nasRepository;
    private final RadiusLifecycleMapper lifecycleMapper;

    /**
     * Sincroniza um contrato com as tabelas do FreeRADIUS (radcheck e radreply)
     */
    @Transactional
    public void syncContractToRadius(UUID contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contrato não encontrado: " + contractId));

        Customer customer = customerRepository.findById(contract.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado: " + contract.getCustomerId()));

        Plan plan = planRepository.findById(contract.getPlanId())
                .orElseThrow(() -> new IllegalArgumentException("Plano não encontrado: " + contract.getPlanId()));

        Optional<OnuProvisioning> onuOpt = onuProvisioningRepository.findByContractId(contractId);
        String username = onuOpt.map(OnuProvisioning::getPppoeUser).orElse(customer.getCpf());
        String password = onuOpt.map(OnuProvisioning::getPppoePassword).orElse("xb123456");

        NasVendorType vendorType = resolveVendorType(onuOpt.orElse(null));

        if (contract.getStatus() == Contract.ContractStatus.ACTIVE) {
            radiusProvisioningService.provisionUser(
                    username,
                    password,
                    plan.getDownloadSpeed(),
                    plan.getUploadSpeed(),
                    vendorType,
                    null,
                    null
            );
            logAudit(contract.getId(), customer.getId(), username, RadiusLifecycleActionType.PROVISIONING_SYNC,
                    "Sincronização de credenciais ativa (" + plan.getDownloadSpeed() + "M/" + plan.getUploadSpeed() + "M)", null, true, null);
        } else if (contract.getStatus() == Contract.ContractStatus.SUSPENDED) {
            radiusProvisioningService.blockUser(username, vendorType, "Contrato Suspenso");
            logAudit(contract.getId(), customer.getId(), username, RadiusLifecycleActionType.AUTO_BLOCK,
                    "Contrato em estado Suspenso", null, true, null);
        }
    }

    /**
     * Executa o corte/bloqueio automático de um contrato por inadimplência
     */
    @Transactional
    public void executeAutoBlock(UUID contractId, String reason) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contrato não encontrado: " + contractId));

        Customer customer = customerRepository.findById(contract.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado: " + contract.getCustomerId()));

        Optional<OnuProvisioning> onuOpt = onuProvisioningRepository.findByContractId(contractId);
        String username = onuOpt.map(OnuProvisioning::getPppoeUser).orElse(customer.getCpf());
        NasVendorType vendorType = resolveVendorType(onuOpt.orElse(null));

        RadiusPolicyConfig policy = getOrCreatePolicyConfig();

        // 1. Atualiza status no banco do ERP
        contract.setStatus(Contract.ContractStatus.SUSPENDED);
        contractRepository.save(contract);

        onuOpt.ifPresent(onu -> {
            onu.setStatus(OnuProvisioning.OnuStatus.BLOCKED);
            onuProvisioningRepository.save(onu);
        });

        // 2. Aplica atributos de bloqueio no FreeRADIUS
        radiusProvisioningService.blockUser(username, vendorType, reason);

        // 3. Dispara PoD (Packet of Disconnect) para derrubar sessão online imediatamente
        String podResult = null;
        if (policy.isSendPodOnBlock()) {
            podResult = disconnectUserSessions(username);
        }

        logAudit(contract.getId(), customer.getId(), username, RadiusLifecycleActionType.AUTO_BLOCK,
                reason, null, true, podResult);

        log.info("Auto-corte aplicado com sucesso ao contrato {} (usuário={}). PoD: {}",
                contract.getContractNumber(), username, podResult);
    }

    /**
     * Executa o desbloqueio imediato do contrato (por pagamento ou em confiança)
     */
    @Transactional
    public void executeInstantUnblock(UUID contractId, String reason, RadiusLifecycleActionType actionType) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contrato não encontrado: " + contractId));

        Customer customer = customerRepository.findById(contract.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado: " + contract.getCustomerId()));

        Plan plan = planRepository.findById(contract.getPlanId())
                .orElseThrow(() -> new IllegalArgumentException("Plano não encontrado: " + contract.getPlanId()));

        Optional<OnuProvisioning> onuOpt = onuProvisioningRepository.findByContractId(contractId);
        String username = onuOpt.map(OnuProvisioning::getPppoeUser).orElse(customer.getCpf());
        NasVendorType vendorType = resolveVendorType(onuOpt.orElse(null));

        RadiusPolicyConfig policy = getOrCreatePolicyConfig();

        // 1. Atualiza status no ERP
        contract.setStatus(Contract.ContractStatus.ACTIVE);
        contractRepository.save(contract);

        onuOpt.ifPresent(onu -> {
            onu.setStatus(OnuProvisioning.OnuStatus.PROVISIONED);
            onuProvisioningRepository.save(onu);
        });

        // 2. Restaura atributos de velocidade integral no FreeRADIUS
        radiusProvisioningService.unblockUser(
                username,
                plan.getDownloadSpeed(),
                plan.getUploadSpeed(),
                vendorType,
                null,
                null
        );

        // 3. Dispara PoD para forçar reconexão na velocidade total
        String podResult = null;
        if (policy.isSendPodOnUnblock()) {
            podResult = disconnectUserSessions(username);
        }

        logAudit(contract.getId(), customer.getId(), username, actionType,
                reason, null, true, podResult);

        log.info("Desbloqueio instantâneo executado para contrato {} (usuário={}). Motivo: {}. PoD: {}",
                contract.getContractNumber(), username, reason, podResult);
    }

    /**
     * Executa ação manual (Bloquear ou Desbloquear) disparada pelo operador
     */
    @Transactional
    public RadiusManualActionResponse executeManualAction(RadiusManualActionRequest request) {
        Contract contract = contractRepository.findById(request.getContractId())
                .orElseThrow(() -> new IllegalArgumentException("Contrato não encontrado: " + request.getContractId()));

        Optional<OnuProvisioning> onuOpt = onuProvisioningRepository.findByContractId(contract.getId());
        Customer customer = customerRepository.findById(contract.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado"));

        String username = onuOpt.map(OnuProvisioning::getPppoeUser).orElse(customer.getCpf());
        String action = request.getAction().toUpperCase();
        String reason = request.getReason() != null ? request.getReason() : "Ação manual de " + action;

        if ("BLOCK".equals(action)) {
            executeAutoBlock(contract.getId(), reason);
            return RadiusManualActionResponse.builder()
                    .contractId(contract.getId())
                    .username(username)
                    .actionApplied("BLOCK")
                    .success(true)
                    .message("Contrato bloqueado com sucesso no FreeRADIUS e sessão desconectada.")
                    .build();
        } else if ("UNBLOCK".equals(action)) {
            executeInstantUnblock(contract.getId(), reason, RadiusLifecycleActionType.MANUAL_UNBLOCK);
            return RadiusManualActionResponse.builder()
                    .contractId(contract.getId())
                    .username(username)
                    .actionApplied("UNBLOCK")
                    .success(true)
                    .message("Contrato desbloqueado com sucesso no FreeRADIUS e velocidade restaurada.")
                    .build();
        } else {
            throw new IllegalArgumentException("Ação desconhecida: " + request.getAction());
        }
    }

    /**
     * Verifica se o cliente não possui mais faturas vencidas além da tolerância
     */
    @Transactional(readOnly = true)
    public boolean isCustomerEligibleForUnblock(UUID customerId) {
        RadiusPolicyConfig policy = getOrCreatePolicyConfig();
        LocalDate cutoffDate = LocalDate.now().minusDays(policy.getToleranceDays());

        List<Invoice> overdueInvoices = invoiceRepository.findByCustomerIdAndStatus(customerId, Invoice.InvoiceStatus.PENDING).stream()
                .filter(inv -> inv.getDueDate().isBefore(cutoffDate))
                .toList();

        return overdueInvoices.isEmpty();
    }

    /**
     * Retorna resumo estatístico de usuários, bloqueios e políticas ativas
     */
    @Transactional(readOnly = true)
    public RadiusLifecycleSummaryResponse getSummary() {
        RadiusPolicyConfig policy = getOrCreatePolicyConfig();

        List<Contract> allContracts = contractRepository.findAll();
        long totalActive = allContracts.stream().filter(c -> c.getStatus() == Contract.ContractStatus.ACTIVE).count();
        long totalBlocked = allContracts.stream().filter(c -> c.getStatus() == Contract.ContractStatus.SUSPENDED).count();

        OffsetDateTime startOfToday = LocalDate.now().atStartOfDay().atOffset(OffsetDateTime.now().getOffset());
        List<RadiusLifecycleLog> todayLogs = lifecycleLogRepository.findAll().stream()
                .filter(l -> l.getCreatedAt() != null && l.getCreatedAt().isAfter(startOfToday))
                .toList();

        long todayBlocks = todayLogs.stream().filter(l -> l.getActionType() == RadiusLifecycleActionType.AUTO_BLOCK).count();
        long todayUnblocks = todayLogs.stream().filter(l -> l.getActionType() == RadiusLifecycleActionType.PAYMENT_UNBLOCK
                || l.getActionType() == RadiusLifecycleActionType.TRUST_UNBLOCK
                || l.getActionType() == RadiusLifecycleActionType.MANUAL_UNBLOCK).count();

        return RadiusLifecycleSummaryResponse.builder()
                .totalPppoeUsers(allContracts.size())
                .totalActiveUsers(totalActive)
                .totalBlockedUsers(totalBlocked)
                .totalTrustUnblocked(0) // pode ser enriquecido
                .todayAutoBlocksCount(todayBlocks)
                .todayUnblocksCount(todayUnblocks)
                .toleranceDays(policy.getToleranceDays())
                .autoBlockEnabled(policy.isAutoBlockEnabled())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<RadiusLifecycleLogResponse> getLogs(Pageable pageable) {
        return lifecycleLogRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(log -> {
                    RadiusLifecycleLogResponse res = lifecycleMapper.toLogResponse(log);
                    customerRepository.findById(log.getCustomerId())
                            .ifPresent(c -> res.setCustomerName(c.getName()));
                    return res;
                });
    }

    @Transactional(readOnly = true)
    public RadiusPolicyConfigResponse getPolicyConfigResponse() {
        return lifecycleMapper.toConfigResponse(getOrCreatePolicyConfig());
    }

    @Transactional
    public RadiusPolicyConfigResponse updatePolicyConfig(RadiusPolicyConfigRequest request) {
        RadiusPolicyConfig config = getOrCreatePolicyConfig();
        lifecycleMapper.updateConfigEntityFromRequest(request, config);
        RadiusPolicyConfig saved = policyConfigRepository.save(config);
        return lifecycleMapper.toConfigResponse(saved);
    }

    private RadiusPolicyConfig getOrCreatePolicyConfig() {
        return policyConfigRepository.findFirstConfig().orElseGet(() -> {
            RadiusPolicyConfig newConfig = RadiusPolicyConfig.builder()
                    .id(UuidCreatorUtils.generateUuidV7())
                    .autoBlockEnabled(true)
                    .toleranceDays(5)
                    .reducedDownloadKbps(256)
                    .reducedUploadKbps(256)
                    .unblockOnPayment(true)
                    .sendPodOnBlock(true)
                    .sendPodOnUnblock(true)
                    .build();
            return policyConfigRepository.save(newConfig);
        });
    }

    private NasVendorType resolveVendorType(OnuProvisioning onu) {
        if (onu != null && onu.getNetworkDeviceId() != null) {
            Optional<Nas> nasOpt = nasRepository.findById(onu.getNetworkDeviceId());
            if (nasOpt.isPresent() && nasOpt.get().getVendorType() != null) {
                return nasOpt.get().getVendorType();
            }
        }
        return NasVendorType.MIKROTIK;
    }

    private String disconnectUserSessions(String username) {
        try {
            RadiusDisconnectRequest req = RadiusDisconnectRequest.builder()
                    .username(username)
                    .build();
            RadiusDisconnectResponse res = radiusSessionService.disconnectUser(req);
            return res.isSuccess() ? "Sessão desconectada via PoD" : "Aviso PoD: " + res.getMessage();
        } catch (Exception e) {
            log.warn("Falha ao enviar pacote PoD para {}: {}", username, e.getMessage());
            return "Erro PoD: " + e.getMessage();
        }
    }

    private void logAudit(UUID contractId, UUID customerId, String username,
                          RadiusLifecycleActionType actionType, String reason, String nasIp,
                          boolean success, String details) {
        RadiusLifecycleLog logEntry = RadiusLifecycleLog.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .contractId(contractId)
                .customerId(customerId)
                .username(username)
                .actionType(actionType)
                .reason(reason)
                .nasIp(nasIp)
                .success(success)
                .details(details)
                .build();
        lifecycleLogRepository.save(logEntry);
    }
}
