package br.dev.xb.isperp.service;

import br.dev.xb.isperp.entity.*;
import br.dev.xb.isperp.repository.*;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import br.dev.xb.isperp.event.GenericDomainEvent;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class WorkOrderRemovalService {

    private final WorkOrderRepository workOrderRepository;
    private final ContractRepository contractRepository;
    private final InvoiceRepository invoiceRepository;
    private final SerializedAssetRepository serializedAssetRepository;
    private final AssetCustodyService assetCustodyService;
    private final LegalCollectionRepository legalCollectionRepository;
    private final DomainEventPublisher domainEventPublisher;

    /**
     * Identifica contratos inadimplentes há mais de 30 dias e gera automaticamente a O.S. de Retirada.
     */
    @Transactional
    public List<WorkOrder> generateRemovalOrdersForOverdueContracts(int thresholdDays) {
        log.info("Verificando inadimplência severa (>= {} dias) para geração de O.S. de Remoção...", thresholdDays);
        LocalDate cutDate = LocalDate.now().minusDays(thresholdDays);
        List<Invoice> overdueInvoices = invoiceRepository.findAll().stream()
                .filter(inv -> (inv.getStatus() == Invoice.InvoiceStatus.OVERDUE || inv.getStatus() == Invoice.InvoiceStatus.PENDING))
                .filter(inv -> inv.getDueDate().isBefore(cutDate))
                .toList();

        List<WorkOrder> generatedOrders = new ArrayList<>();

        for (Invoice invoice : overdueInvoices) {
            Contract contract = contractRepository.findById(invoice.getContractId()).orElse(null);
            if (contract == null || contract.getStatus() == Contract.ContractStatus.CANCELED) {
                continue;
            }

            // Verifica se já existe O.S. de retirada aberta para este contrato
            List<WorkOrder> existingRemoval = workOrderRepository.findByContractIdAndType(contract.getId(), WorkOrder.WorkOrderType.RETIRADA);
            boolean hasActiveRemoval = existingRemoval.stream().anyMatch(wo ->
                    wo.getStatus() == WorkOrder.WorkOrderStatus.PENDING_SCHEDULE ||
                    wo.getStatus() == WorkOrder.WorkOrderStatus.SCHEDULED ||
                    wo.getStatus() == WorkOrder.WorkOrderStatus.IN_PROGRESS
            );

            if (!hasActiveRemoval) {
                long daysOverdue = ChronoUnit.DAYS.between(invoice.getDueDate(), LocalDate.now());
                WorkOrder removalOrder = WorkOrder.builder()
                        .id(UuidCreatorUtils.generateUuidV7())
                        .contractId(contract.getId())
                        .customerId(contract.getCustomerId())
                        .type(WorkOrder.WorkOrderType.RETIRADA)
                        .status(WorkOrder.WorkOrderStatus.PENDING_SCHEDULE)
                        .scheduledDate(LocalDate.now().plusDays(2))
                        .scheduledPeriod("MANHA")
                        .notes("Inadimplência de " + daysOverdue + " dias. Recolhimento compulsório de ONT/Roteador em comodato.")
                        .build();

                WorkOrder saved = workOrderRepository.save(removalOrder);
                generatedOrders.add(saved);
                log.warn("O.S. de RETIRADA gerada para contrato {}: {} dias de atraso na fatura {}",
                        contract.getContractNumber(), daysOverdue, invoice.getId());

                // Publica evento de domínio REMOVAL_ORDER_GENERATED
                Map<String, Object> payload = Map.of(
                        "workOrderId", saved.getId().toString(),
                        "contractId", contract.getId().toString(),
                        "contractNumber", contract.getContractNumber(),
                        "customerId", contract.getCustomerId().toString(),
                        "scheduledDate", saved.getScheduledDate() != null ? saved.getScheduledDate().toString() : "",
                        "scheduledPeriod", saved.getScheduledPeriod() != null ? saved.getScheduledPeriod() : "",
                        "daysOverdue", daysOverdue,
                        "notes", saved.getNotes() != null ? saved.getNotes() : ""
                );
                domainEventPublisher.publish(GenericDomainEvent.builder()
                        .eventType("REMOVAL_ORDER_GENERATED")
                        .aggregateType("WorkOrder")
                        .aggregateId(saved.getId().toString())
                        .payload(payload)
                        .build());
            }
        }

        return generatedOrders;
    }

    /**
     * Conclui a O.S. de Retirada com SUCESSO (equipamento recolhido e devolvido ao almoxarifado via logística reversa).
     */
    @Transactional
    public WorkOrder completeSuccessfulRemoval(UUID workOrderId, UUID warehouseId, @Nullable String photoUrl, @Nullable String notes) {
        WorkOrder wo = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Ordem de serviço não encontrada: " + workOrderId));

        wo.setStatus(WorkOrder.WorkOrderStatus.COMPLETED);
        wo.setCompletedAt(LocalDateTime.now());
        wo.setInstallationPhotoUrl(photoUrl);
        wo.setNotes((wo.getNotes() != null ? wo.getNotes() + "\n" : "") + "Remoção de equipamento executada com sucesso. Equipamento recuperado e recolhido. " + (notes != null ? notes : ""));
        WorkOrder savedWo = workOrderRepository.save(wo);

        // Devolve o equipamento serializado ao depósito
        List<SerializedAsset> assets = serializedAssetRepository.findByCurrentContractId(wo.getContractId());
        if (assets.isEmpty()) {
            assets = serializedAssetRepository.findByCurrentCustomerId(wo.getCustomerId());
        }

        for (SerializedAsset asset : assets) {
            assetCustodyService.returnAssetFromWorkOrder(
                    asset.getId(),
                    warehouseId,
                    false,
                    photoUrl,
                    "Logística reversa bem-sucedida via O.S. de Retirada " + wo.getId()
            );
        }

        // Cancela o contrato oficialmente
        contractRepository.findById(wo.getContractId()).ifPresent(c -> {
            c.setStatus(Contract.ContractStatus.CANCELED);
            contractRepository.save(c);
            log.info("Contrato {} cancelado após recolhimento com sucesso de seus equipamentos comodatados.", c.getContractNumber());
        });

        // Publica evento de domínio REMOVAL_ORDER_COMPLETED
        Map<String, Object> payload = Map.of(
                "workOrderId", savedWo.getId().toString(),
                "contractId", savedWo.getContractId().toString(),
                "customerId", savedWo.getCustomerId().toString(),
                "warehouseId", warehouseId.toString(),
                "completedAt", savedWo.getCompletedAt() != null ? savedWo.getCompletedAt().toString() : LocalDateTime.now().toString(),
                "notes", savedWo.getNotes() != null ? savedWo.getNotes() : ""
        );
        domainEventPublisher.publish(GenericDomainEvent.builder()
                .eventType("REMOVAL_ORDER_COMPLETED")
                .aggregateType("WorkOrder")
                .aggregateId(savedWo.getId().toString())
                .payload(payload)
                .build());

        return savedWo;
    }

    /**
     * Conclui a O.S. de Retirada como INFRUTÍFERA (cliente recusou, mudou-se ou não devolveu).
     * Cancela o contrato e encaminha o débito + valor do equipamento para Cobrança Jurídica / SPC/Serasa.
     */
    @Transactional
    public LegalCollectionRecord completeUnsuccessfulRemoval(
            UUID workOrderId,
            String unsuccessReason,
            @Nullable String photoUrl,
            @Nullable String notes
    ) {
        WorkOrder wo = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Ordem de serviço não encontrada: " + workOrderId));

        wo.setStatus(WorkOrder.WorkOrderStatus.INFRUTIFERA);
        wo.setUnsuccessReason(unsuccessReason);
        wo.setCompletedAt(LocalDateTime.now());
        wo.setInstallationPhotoUrl(photoUrl);
        wo.setNotes((wo.getNotes() != null ? wo.getNotes() + "\n" : "") + "Tentativa de remoção infrutífera: " + unsuccessReason + ". " + (notes != null ? notes : ""));
        workOrderRepository.save(wo);

        // Cancela o contrato por inadimplência com perda de comodato
        contractRepository.findById(wo.getContractId()).ifPresent(c -> {
            c.setStatus(Contract.ContractStatus.CANCELED);
            contractRepository.save(c);
            log.warn("Contrato {} cancelado com tentativa de retirada infrutífera: {}", c.getContractNumber(), unsuccessReason);
        });

        // 1. Calcula o total das faturas em atraso
        BigDecimal totalDebt = invoiceRepository.findByContractId(wo.getContractId()).stream()
                .filter(inv -> inv.getStatus() == Invoice.InvoiceStatus.OVERDUE || inv.getStatus() == Invoice.InvoiceStatus.PENDING)
                .map(Invoice::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. Apura o valor de reposição do equipamento comodatado não devolvido
        List<SerializedAsset> assets = serializedAssetRepository.findByCurrentContractId(wo.getContractId());
        if (assets.isEmpty()) {
            assets = serializedAssetRepository.findByCurrentCustomerId(wo.getCustomerId());
        }

        BigDecimal equipmentValue = assets.stream()
                .map(SerializedAsset::getReplacementValue)
                .filter(val -> val != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (equipmentValue.compareTo(BigDecimal.ZERO) == 0) {
            equipmentValue = new BigDecimal("420.00"); // Valor padrão de reposição de ONT Wi-Fi 6
        }

        BigDecimal totalClaim = totalDebt.add(equipmentValue);

        // 3. Cria o processo de Proteção ao Crédito e Execução Jurídica
        LegalCollectionRecord collectionRecord = LegalCollectionRecord.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .customerId(wo.getCustomerId())
                .contractId(wo.getContractId())
                .workOrderId(wo.getId())
                .totalDebtInvoices(totalDebt)
                .equipmentIndemnityAmount(equipmentValue)
                .totalClaimAmount(totalClaim)
                .status(LegalCollectionRecord.LegalCollectionStatus.PENDING_BUREAU_SUBMISSION)
                .evidenceNotes("Tentativa de recolha de equipamento frustrada. Motivo: " + unsuccessReason +
                        (photoUrl != null ? " | Evidência Fotográfica: " + photoUrl : "") +
                        (notes != null ? " | " + notes : ""))
                .build();

        LegalCollectionRecord saved = legalCollectionRepository.save(collectionRecord);
        log.warn("PROCESSO JURÍDICO / SPC GERADO para cliente {}: Total Executável = R$ {} (Faturas: R$ {}, ONT: R$ {})",
                wo.getCustomerId(), totalClaim, totalDebt, equipmentValue);

        // Publica evento de domínio LEGAL_COLLECTION_RECORD_CREATED
        Map<String, Object> payload = Map.of(
                "legalCollectionId", saved.getId().toString(),
                "workOrderId", wo.getId().toString(),
                "contractId", wo.getContractId().toString(),
                "customerId", wo.getCustomerId().toString(),
                "totalDebtInvoices", totalDebt.toString(),
                "equipmentIndemnityAmount", equipmentValue.toString(),
                "totalClaimAmount", totalClaim.toString(),
                "unsuccessReason", unsuccessReason,
                "status", saved.getStatus().name()
        );
        domainEventPublisher.publish(GenericDomainEvent.builder()
                .eventType("LEGAL_COLLECTION_RECORD_CREATED")
                .aggregateType("LegalCollectionRecord")
                .aggregateId(saved.getId().toString())
                .payload(payload)
                .build());

        return saved;
    }
}
