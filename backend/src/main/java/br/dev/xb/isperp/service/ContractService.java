package br.dev.xb.isperp.service;

import br.dev.xb.isperp.api.dto.ContractAddressChangeRequest;
import br.dev.xb.isperp.api.dto.ContractOwnershipTransferRequest;
import br.dev.xb.isperp.api.dto.ContractPlanChangeRequest;
import br.dev.xb.isperp.dto.CreateSignatureSessionRequest;
import br.dev.xb.isperp.dto.SignatureSessionResponse;
import br.dev.xb.isperp.entity.*;
import br.dev.xb.isperp.entity.financial.FeeStatus;
import br.dev.xb.isperp.repository.*;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@SuppressWarnings("null")
public class ContractService {

    private final ContractRepository contractRepository;

    @Nullable
    private PlanRepository planRepository;

    @Nullable
    private CustomerRepository customerRepository;

    @Nullable
    private InvoiceRepository invoiceRepository;

    @Nullable
    private WorkOrderRepository workOrderRepository;

    @Nullable
    private ContractLifecycleEventRepository lifecycleEventRepository;

    @Nullable
    private RadiusLifecycleService radiusLifecycleService;

    @Nullable
    private ElectronicSignatureService electronicSignatureService;

    public ContractService(ContractRepository contractRepository) {
        this.contractRepository = contractRepository;
    }

    @Autowired
    public ContractService(
            ContractRepository contractRepository,
            @Nullable PlanRepository planRepository,
            @Nullable CustomerRepository customerRepository,
            @Nullable InvoiceRepository invoiceRepository,
            @Nullable WorkOrderRepository workOrderRepository,
            @Nullable ContractLifecycleEventRepository lifecycleEventRepository,
            @Nullable RadiusLifecycleService radiusLifecycleService,
            @Nullable ElectronicSignatureService electronicSignatureService) {
        this.contractRepository = contractRepository;
        this.planRepository = planRepository;
        this.customerRepository = customerRepository;
        this.invoiceRepository = invoiceRepository;
        this.workOrderRepository = workOrderRepository;
        this.lifecycleEventRepository = lifecycleEventRepository;
        this.radiusLifecycleService = radiusLifecycleService;
        this.electronicSignatureService = electronicSignatureService;
    }

    public record LifecycleActionResult(Contract contract, ContractLifecycleEvent event) {}

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

    public List<ContractLifecycleEvent> getLifecycleEvents(UUID contractId) {
        if (lifecycleEventRepository == null) {
            return List.of();
        }
        return lifecycleEventRepository.findByContractIdOrderByCreatedAtDesc(contractId);
    }

    /**
     * 1. Upgrade / Downgrade de Plano com cálculo Pro-Rata Die (base 30 dias) e sincronização FreeRADIUS.
     */
    @Transactional
    public LifecycleActionResult changePlan(UUID contractId, ContractPlanChangeRequest request) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contrato não encontrado: " + contractId));

        if (planRepository == null) {
            throw new IllegalStateException("PlanRepository não configurado");
        }
        Plan newPlan = planRepository.findById(request.getNewPlanId())
                .orElseThrow(() -> new IllegalArgumentException("Novo plano não encontrado: " + request.getNewPlanId()));

        UUID previousPlanId = contract.getPlanId();
        BigDecimal oldFee = contract.getMonthlyFee() != null ? contract.getMonthlyFee() : BigDecimal.ZERO;
        BigDecimal newFee = newPlan.getPrice();

        int remainingDays = calculateRemainingDaysInCycle(contract.getDueDay() != null ? contract.getDueDay() : 10, LocalDate.now());
        boolean applyProRata = request.getApplyProRata() == null || request.getApplyProRata();

        BigDecimal proRataDelta = BigDecimal.ZERO;
        UUID adjustedInvoiceId = null;

        if (applyProRata) {
            proRataDelta = newFee.subtract(oldFee)
                    .multiply(BigDecimal.valueOf(remainingDays))
                    .divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP);

            if (invoiceRepository != null && proRataDelta.compareTo(BigDecimal.ZERO) != 0) {
                LocalDate today = LocalDate.now();
                Optional<Invoice> openInvoiceOpt = invoiceRepository.findByContractIdOrderByDueDateDesc(contractId)
                        .stream()
                        .filter(inv -> inv.getStatus() == Invoice.InvoiceStatus.PENDING && !inv.getDueDate().isBefore(today))
                        .findFirst();

                if (openInvoiceOpt.isPresent()) {
                    Invoice openInv = openInvoiceOpt.get();
                    BigDecimal updatedAmount = openInv.getAmount().add(proRataDelta);
                    if (updatedAmount.compareTo(BigDecimal.valueOf(0.01)) < 0) {
                        updatedAmount = BigDecimal.valueOf(0.01);
                    }
                    openInv.setAmount(updatedAmount);
                    if (openInv.getCompositionItems() == null) {
                        openInv.setCompositionItems(new ArrayList<>());
                    }
                    openInv.getCompositionItems().add(InvoiceCompositionItem.builder()
                            .id(UuidCreatorUtils.generateUuidV7())
                            .invoice(openInv)
                            .name("Ajuste Pro-Rata Die (" + remainingDays + "d) — Mudança p/ " + newPlan.getName())
                            .category(PlanCompositionCategory.SCM)
                            .amount(proRataDelta)
                            .fiscalCode("5307")
                            .createdAt(LocalDateTime.now())
                            .build());
                    openInv.recalculateCompositionTotals();
                    invoiceRepository.save(openInv);
                    adjustedInvoiceId = openInv.getId();
                } else {
                    BigDecimal currentPending = contract.getPendingProRataAdjustment() != null
                            ? contract.getPendingProRataAdjustment()
                            : BigDecimal.ZERO;
                    contract.setPendingProRataAdjustment(currentPending.add(proRataDelta));
                }
            }
        }

        contract.setPlanId(newPlan.getId());
        contract.setMonthlyFee(newFee);
        Contract savedContract = contractRepository.save(contract);

        String rateLimitApplied = newPlan.getDownloadSpeed() + "M/" + newPlan.getUploadSpeed() + "M";
        if (radiusLifecycleService != null) {
            try {
                radiusLifecycleService.syncContractToRadius(contractId);
            } catch (Exception e) {
                log.warn("Aviso ao sincronizar FreeRADIUS na troca de plano do contrato {}: {}", contractId, e.getMessage());
            }
        }

        ContractLifecycleEvent event = ContractLifecycleEvent.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .contractId(contractId)
                .eventType(ContractLifecycleEvent.EventType.PLAN_CHANGE)
                .previousPlanId(previousPlanId)
                .newPlanId(newPlan.getId())
                .oldMonthlyFee(oldFee)
                .newMonthlyFee(newFee)
                .remainingDaysInCycle(remainingDays)
                .proRataAdjustmentAmount(proRataDelta)
                .adjustedInvoiceId(adjustedInvoiceId)
                .radiusRateLimitApplied(rateLimitApplied)
                .notes(request.getReason())
                .createdAt(LocalDateTime.now())
                .build();

        if (lifecycleEventRepository != null) {
            event = lifecycleEventRepository.save(event);
        }

        return new LifecycleActionResult(savedContract, event);
    }

    /**
     * 2. Mudança de Endereço com abertura automática de Ordem de Serviço e gestão de taxa/isenção.
     */
    @Transactional
    public LifecycleActionResult changeAddress(UUID contractId, ContractAddressChangeRequest request) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contrato não encontrado: " + contractId));

        String previousAddress = contract.getInstallationAddress();
        contract.setInstallationAddress(request.getNewInstallationAddress().trim());
        if (request.getNewCity() != null) {
            contract.setCity(request.getNewCity().trim());
        }
        if (request.getNewState() != null) {
            contract.setState(request.getNewState().trim());
        }
        if (request.getNewZipCode() != null) {
            contract.setZipCode(request.getNewZipCode().trim());
        }
        if (request.getNewCtoId() != null) {
            contract.setCtoId(request.getNewCtoId());
        }
        if (request.getNewCtoPortNumber() != null) {
            contract.setCtoPortNumber(request.getNewCtoPortNumber());
        }

        Contract savedContract = contractRepository.save(contract);

        UUID createdWorkOrderId = null;
        if (workOrderRepository != null) {
            BigDecimal feeAmount = request.getServiceFee() != null
                    ? BigDecimal.valueOf(request.getServiceFee()).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            boolean requestWaiver = Boolean.TRUE.equals(request.getRequestWaiver());

            FeeStatus feeStatus = FeeStatus.NOT_APPLICABLE;
            if (feeAmount.compareTo(BigDecimal.ZERO) > 0) {
                feeStatus = requestWaiver ? FeeStatus.PENDING_WAIVER_APPROVAL : FeeStatus.BILLABLE;
            }

            WorkOrder workOrder = WorkOrder.builder()
                    .id(UuidCreatorUtils.generateUuidV7())
                    .contractId(contractId)
                    .customerId(contract.getCustomerId())
                    .type(WorkOrder.WorkOrderType.INSTALACAO)
                    .status(WorkOrder.WorkOrderStatus.PENDING_SCHEDULE)
                    .ctoId(contract.getCtoId())
                    .ctoPortNumber(contract.getCtoPortNumber())
                    .standardFeeAmount(feeAmount)
                    .feeStatus(feeStatus)
                    .waiverReason(request.getWaiverReason())
                    .notes("Mudança de Endereço: de [" + previousAddress + "] para [" + contract.getInstallationAddress() + "]"
                            + (request.getNotes() != null && !request.getNotes().isBlank() ? " — " + request.getNotes() : ""))
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            WorkOrder savedWo = workOrderRepository.save(workOrder);
            createdWorkOrderId = savedWo.getId();
        }

        ContractLifecycleEvent event = ContractLifecycleEvent.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .contractId(contractId)
                .eventType(ContractLifecycleEvent.EventType.ADDRESS_CHANGE)
                .previousAddress(previousAddress)
                .newAddress(contract.getInstallationAddress())
                .workOrderId(createdWorkOrderId)
                .notes(request.getNotes() != null ? request.getNotes() : request.getWaiverReason())
                .createdAt(LocalDateTime.now())
                .build();

        if (lifecycleEventRepository != null) {
            event = lifecycleEventRepository.save(event);
        }

        return new LifecycleActionResult(savedContract, event);
    }

    /**
     * 3. Troca de Titularidade com migração integral de faturas PENDING/OVERDUE para o novo CPF/CNPJ e nova Assinatura Forense.
     */
    @Transactional
    public LifecycleActionResult transferOwnership(UUID contractId, ContractOwnershipTransferRequest request) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contrato não encontrado: " + contractId));

        if (customerRepository != null) {
            customerRepository.findById(request.getNewCustomerId())
                    .orElseThrow(() -> new IllegalArgumentException("Novo titular não encontrado: " + request.getNewCustomerId()));
        }

        UUID previousCustomerId = contract.getCustomerId();
        UUID newCustomerId = request.getNewCustomerId();

        contract.setCustomerId(newCustomerId);
        Contract savedContract = contractRepository.save(contract);

        int migratedCount = 0;
        if (invoiceRepository != null) {
            List<Invoice> contractInvoices = invoiceRepository.findByContractIdOrderByDueDateDesc(contractId);
            for (Invoice inv : contractInvoices) {
                if (inv.getStatus() == Invoice.InvoiceStatus.PENDING || inv.getStatus() == Invoice.InvoiceStatus.OVERDUE) {
                    inv.setCustomerId(newCustomerId);
                    invoiceRepository.save(inv);
                    migratedCount++;
                }
            }
        }

        UUID signatureSessionId = null;
        String signatureUrl = null;
        boolean genSignature = request.getGenerateForensicSignature() == null || request.getGenerateForensicSignature();
        if (genSignature && electronicSignatureService != null) {
            try {
                CreateSignatureSessionRequest sigReq = CreateSignatureSessionRequest.builder()
                        .contractId(contractId)
                        .symbolicAmount(BigDecimal.valueOf(1.00))
                        .build();
                SignatureSessionResponse sigRes = electronicSignatureService.createSignatureSession(sigReq, null);
                signatureSessionId = sigRes.getId();
                signatureUrl = sigRes.getSignatureUrl();
            } catch (Exception e) {
                log.warn("Aviso ao gerar sessão de assinatura forense na troca de titularidade do contrato {}: {}", contractId, e.getMessage());
            }
        }

        ContractLifecycleEvent event = ContractLifecycleEvent.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .contractId(contractId)
                .eventType(ContractLifecycleEvent.EventType.OWNERSHIP_TRANSFER)
                .previousCustomerId(previousCustomerId)
                .newCustomerId(newCustomerId)
                .migratedInvoicesCount(migratedCount)
                .signatureSessionId(signatureSessionId)
                .signatureUrl(signatureUrl)
                .notes(request.getReason())
                .createdAt(LocalDateTime.now())
                .build();

        if (lifecycleEventRepository != null) {
            event = lifecycleEventRepository.save(event);
        }

        return new LifecycleActionResult(savedContract, event);
    }

    public static int calculateRemainingDaysInCycle(int dueDay, LocalDate today) {
        int safeDueDay = Math.max(1, Math.min(28, dueDay));
        LocalDate nextDueDate;
        if (today.getDayOfMonth() < safeDueDay) {
            nextDueDate = today.withDayOfMonth(safeDueDay);
        } else {
            nextDueDate = today.plusMonths(1).withDayOfMonth(safeDueDay);
        }
        long days = ChronoUnit.DAYS.between(today, nextDueDate);
        return (int) Math.max(1, Math.min(30, days));
    }
}
