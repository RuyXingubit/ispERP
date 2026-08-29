package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.ChangePasswordRequest;
import br.dev.xb.isperp.dto.ClientPortalDashboardDTO;
import br.dev.xb.isperp.dto.UpdateClientProfileRequest;
import br.dev.xb.isperp.entity.*;
import br.dev.xb.isperp.exception.ResourceNotFoundException;
import br.dev.xb.isperp.repository.*;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class ClientPortalService {

    private final CustomerRepository customerRepository;
    private final ContractRepository contractRepository;
    private final PlanRepository planRepository;
    private final InvoiceRepository invoiceRepository;
    private final TrustUnblockRepository trustUnblockRepository;
    private final PlanUpgradeRequestRepository planUpgradeRequestRepository;
    private final UserRepository userRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Retorna a visão completa do painel do assinante.
     */
    @Transactional(readOnly = true)
    public ClientPortalDashboardDTO getClientDashboard(@NonNull UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado: " + customerId));

        List<Contract> contracts = contractRepository.findByCustomerId(customerId);
        Contract contract = contracts.stream()
                .filter(c -> c.getStatus() == Contract.ContractStatus.ACTIVE || c.getStatus() == Contract.ContractStatus.SUSPENDED)
                .findFirst()
                .orElse(contracts.isEmpty() ? null : contracts.get(0));

        Plan currentPlan = null;
        if (contract != null) {
            currentPlan = planRepository.findById(contract.getPlanId()).orElse(null);
        }

        List<Plan> availableUpgradePlans = planRepository.findByActiveTrue();
        if (currentPlan != null) {
            final UUID currentPlanId = currentPlan.getId();
            availableUpgradePlans = availableUpgradePlans.stream()
                    .filter(p -> !p.getId().equals(currentPlanId))
                    .toList();
        }

        List<Invoice> allInvoices = invoiceRepository.findByCustomerIdOrderByDueDateDesc(customerId);
        LocalDate today = LocalDate.now();

        List<Invoice> pendingInvoices = allInvoices.stream()
                .filter(i -> i.getStatus() == Invoice.InvoiceStatus.PENDING && !i.getDueDate().isBefore(today))
                .toList();

        List<Invoice> overdueInvoices = allInvoices.stream()
                .filter(i -> i.getStatus() == Invoice.InvoiceStatus.PENDING && i.getDueDate().isBefore(today)
                        || i.getStatus() == Invoice.InvoiceStatus.OVERDUE)
                .toList();

        List<Invoice> paidInvoices = allInvoices.stream()
                .filter(i -> i.getStatus() == Invoice.InvoiceStatus.PAID)
                .toList();

        boolean isBlocked = contract != null && contract.getStatus() == Contract.ContractStatus.SUSPENDED;
        boolean canRequestTrustUnblock = isBlocked && canPerformTrustUnblock(contract.getId());

        String statusMsg = isBlocked
                ? "Sua conexão está temporariamente suspensa por pendência financeira."
                : "Sua conexão está ativa e operando normalmente.";

        return ClientPortalDashboardDTO.builder()
                .customer(customer)
                .contract(contract)
                .currentPlan(currentPlan)
                .availableUpgradePlans(availableUpgradePlans)
                .pendingInvoices(pendingInvoices)
                .overdueInvoices(overdueInvoices)
                .paidInvoices(paidInvoices)
                .isConnectionBlocked(isBlocked)
                .canRequestTrustUnblock(canRequestTrustUnblock)
                .connectionStatusMessage(statusMsg)
                .build();
    }

    /**
     * Atualiza dados de contato do cliente.
     */
    @Transactional
    public Customer updateProfile(@NonNull UUID customerId, @NonNull UpdateClientProfileRequest request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado: " + customerId));

        if (request.getName() != null && !request.getName().isBlank()) {
            customer.setName(request.getName());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            customer.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            customer.setPhone(request.getPhone());
        }
        if (request.getAddress() != null) {
            customer.setAddress(request.getAddress());
        }
        if (request.getCity() != null) {
            customer.setCity(request.getCity());
        }
        if (request.getState() != null) {
            customer.setState(request.getState());
        }
        if (request.getZipCode() != null) {
            customer.setZipCode(request.getZipCode());
        }

        return customerRepository.save(customer);
    }

    /**
     * Altera a senha do assinante no portal.
     */
    @Transactional
    public void changePassword(@NonNull UUID customerId, @NonNull ChangePasswordRequest request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado: " + customerId));

        // Busca o usuário associado (pelo email do cliente)
        User user = userRepository.findByEmail(customer.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário de acesso não encontrado"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Senha atual informada é incorreta");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    /**
     * Executa solicitação de Upgrade de Plano pelo cliente.
     */
    @Transactional
    public Contract requestPlanUpgrade(@NonNull UUID customerId, @NonNull UUID contractId, @NonNull UUID newPlanId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contrato não encontrado: " + contractId));

        if (!contract.getCustomerId().equals(customerId)) {
            throw new IllegalArgumentException("Contrato não pertence ao cliente logado");
        }

        Plan newPlan = planRepository.findById(newPlanId)
                .orElseThrow(() -> new ResourceNotFoundException("Novo plano não encontrado: " + newPlanId));

        UUID oldPlanId = contract.getPlanId();
        contract.setPlanId(newPlan.getId());
        contract.setMonthlyFee(newPlan.getPrice());
        contractRepository.save(contract);

        // Salva registro de upgrade
        PlanUpgradeRequest upgradeRequest = PlanUpgradeRequest.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .contractId(contract.getId())
                .oldPlanId(oldPlanId)
                .newPlanId(newPlan.getId())
                .status("COMPLETED")
                .build();
        planUpgradeRequestRepository.save(upgradeRequest);

        // Emite evento PLAN_UPGRADED para reprovisionamento automático na OLT
        String payload = String.format(
                "{\"contractId\":\"%s\",\"customerId\":\"%s\",\"newPlanId\":\"%s\",\"downloadSpeed\":%d,\"uploadSpeed\":%d}",
                contract.getId(), customerId, newPlan.getId(), newPlan.getDownloadSpeed(), newPlan.getUploadSpeed()
        );

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .aggregateType("CONTRACT")
                .aggregateId(contract.getId().toString())
                .eventType("PLAN_UPGRADED")
                .payload(payload)
                .status(OutboxEvent.OutboxStatus.PENDING)
                .build();
        outboxEventRepository.save(outboxEvent);

        log.info("Upgrade de plano concluído com sucesso para o contrato {}: Novo plano {}",
                contract.getContractNumber(), newPlan.getName());

        return contract;
    }

    /**
     * Solicita o Desbloqueio em Confiança (Promessa de Pagamento por 48 horas).
     */
    @Transactional
    public TrustUnblock requestTrustUnblock(@NonNull UUID customerId, @NonNull UUID contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contrato não encontrado: " + contractId));

        if (!contract.getCustomerId().equals(customerId)) {
            throw new IllegalArgumentException("Contrato não pertence ao cliente logado");
        }

        if (!canPerformTrustUnblock(contractId)) {
            throw new IllegalStateException("Desbloqueio em confiança já foi utilizado recentemente.");
        }

        TrustUnblock trustUnblock = TrustUnblock.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .contractId(contract.getId())
                .requestedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(48))
                .status("ACTIVE")
                .build();
        trustUnblock = trustUnblockRepository.save(trustUnblock);

        // Altera status do contrato para ACTIVE temporariamente
        contract.setStatus(Contract.ContractStatus.ACTIVE);
        contractRepository.save(contract);

        // Emite evento para OLT/concentrador restabelecer o sinal do assinante
        String payload = String.format("{\"contractId\":\"%s\",\"customerId\":\"%s\",\"reason\":\"TRUST_UNBLOCK\"}",
                contract.getId(), customerId);

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .aggregateType("CONTRACT")
                .aggregateId(contract.getId().toString())
                .eventType("INTERNET_ACCESS_UNBLOCKED")
                .payload(payload)
                .status(OutboxEvent.OutboxStatus.PENDING)
                .build();
        outboxEventRepository.save(outboxEvent);

        log.info("Desbloqueio em confiança concedido com sucesso para o contrato {}", contract.getContractNumber());

        return trustUnblock;
    }

    private boolean canPerformTrustUnblock(UUID contractId) {
        Optional<TrustUnblock> lastActive = trustUnblockRepository
                .findFirstByContractIdAndStatusOrderByRequestedAtDesc(contractId, "ACTIVE");
        return lastActive.isEmpty();
    }
}
