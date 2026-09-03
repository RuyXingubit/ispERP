package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.*;
import br.dev.xb.isperp.entity.*;
import br.dev.xb.isperp.exception.ResourceNotFoundException;
import br.dev.xb.isperp.repository.*;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.jspecify.annotations.Nullable;

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
    public ClientPortalDashboardDTO getClientDashboard(UUID customerId) {
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
        boolean canRequestTrustUnblock = isBlocked && contract != null && canPerformTrustUnblock(contract.getId());

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
    public Customer updateProfile(UUID customerId, UpdateClientProfileRequest request) {
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
    public void changePassword(UUID customerId, ChangePasswordRequest request) {
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
    public Contract requestPlanUpgrade(UUID customerId, @Nullable UUID contractId, @Nullable UUID newPlanId) {
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
    public TrustUnblock requestTrustUnblock(UUID customerId, @Nullable UUID contractId) {
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

    /**
     * Autentica o cliente por CPF/CNPJ e valida PIN de 4 dígitos.
     */
    @Transactional(readOnly = true)
    public ClientAuthResponse authenticateClient(ClientAuthRequest request) {
        String rawDoc = request.getDocument();
        String cleanDoc = rawDoc.replaceAll("[^0-9]", "");

        Customer customer = customerRepository.findByCpfOrCleanDocument(rawDoc, cleanDoc)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não localizado com o CPF/CNPJ informado."));

        if (!Boolean.TRUE.equals(customer.getActive())) {
            throw new IllegalArgumentException("Cadastro de cliente inativo. Por favor, contate nosso suporte.");
        }

        boolean hasPin = customer.getPortalPin() != null && !customer.getPortalPin().isBlank();

        // Se o cliente tem PIN cadastrado
        if (hasPin) {
            if (request.getPin() == null || request.getPin().isBlank()) {
                return ClientAuthResponse.builder()
                        .status("PIN_REQUIRED")
                        .message("Informe seu PIN de 4 dígitos para prosseguir.")
                        .customerId(customer.getId())
                        .customerName(customer.getName())
                        .maskedDocument(maskDocument(cleanDoc))
                        .hasPin(true)
                        .build();
            }

            // Validar o PIN
            boolean pinMatches = passwordEncoder.matches(request.getPin(), customer.getPortalPin())
                    || request.getPin().equals(customer.getPortalPin()); // fallback para PIN legado não hasheado

            if (!pinMatches) {
                throw new IllegalArgumentException("PIN incorreto. Tente novamente.");
            }

            if (Boolean.TRUE.equals(customer.getPinForceChange())) {
                return ClientAuthResponse.builder()
                        .status("FORCE_CHANGE_PIN")
                        .message("Por segurança, cadastre um novo PIN de 4 dígitos para o seu acesso.")
                        .customerId(customer.getId())
                        .customerName(customer.getName())
                        .maskedDocument(maskDocument(cleanDoc))
                        .hasPin(true)
                        .build();
            }
        }

        // Autenticado com sucesso
        return ClientAuthResponse.builder()
                .status("AUTHENTICATED")
                .message("Acesso liberado com sucesso.")
                .customerId(customer.getId())
                .customerName(customer.getName())
                .maskedDocument(maskDocument(cleanDoc))
                .hasPin(hasPin)
                .customer(customer)
                .build();
    }

    /**
     * Define ou atualiza o PIN de 4 dígitos do assinante.
     */
    @Transactional
    public void setPin(SetClientPinRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado: " + request.getCustomerId()));

        // Se já tiver PIN e não for force change, valida o PIN atual
        if (customer.getPortalPin() != null && !customer.getPortalPin().isBlank()
                && !Boolean.TRUE.equals(customer.getPinForceChange())
                && request.getCurrentPin() != null && !request.getCurrentPin().isBlank()) {
            boolean pinMatches = passwordEncoder.matches(request.getCurrentPin(), customer.getPortalPin())
                    || request.getCurrentPin().equals(customer.getPortalPin());
            if (!pinMatches) {
                throw new IllegalArgumentException("PIN atual incorreto.");
            }
        }

        customer.setPortalPin(passwordEncoder.encode(request.getNewPin()));
        customer.setPinForceChange(false);
        customerRepository.save(customer);
        log.info("PIN de 4 dígitos atualizado com sucesso para o cliente {}", customer.getId());
    }

    /**
     * Operador administrativo reseta ou define um PIN temporário para o assinante.
     */
    @Transactional
    public void resetPinByOperator(UUID customerId, String temporaryPin, boolean forceChange) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado: " + customerId));

        customer.setPortalPin(passwordEncoder.encode(temporaryPin));
        customer.setPinForceChange(forceChange);
        customerRepository.save(customer);
        log.info("PIN resetado pelo operador para o cliente {}", customerId);
    }

    private String maskDocument(String doc) {
        if (doc == null || doc.length() < 6) return "***";
        if (doc.length() == 11) {
            // CPF: 123.***.***-00
            return doc.substring(0, 3) + ".***.***-" + doc.substring(9);
        } else if (doc.length() == 14) {
            // CNPJ: 12.***.***/****-00
            return doc.substring(0, 2) + ".***.***/****-" + doc.substring(12);
        }
        return doc.substring(0, 2) + "***" + doc.substring(doc.length() - 2);
    }
}
