package br.dev.xb.isperp.service;

import br.dev.xb.isperp.entity.*;
import br.dev.xb.isperp.repository.*;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkOrderRemovalServiceTest {

    @Mock
    private WorkOrderRepository workOrderRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private SerializedAssetRepository serializedAssetRepository;

    @Mock
    private AssetCustodyService assetCustodyService;

    @Mock
    private LegalCollectionRepository legalCollectionRepository;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @InjectMocks
    private WorkOrderRemovalService workOrderRemovalService;

    private UUID contractId;
    private UUID customerId;
    private UUID workOrderId;
    private UUID warehouseId;
    private Contract contract;

    @BeforeEach
    void setUp() {
        contractId = UuidCreatorUtils.generateUuidV7();
        customerId = UuidCreatorUtils.generateUuidV7();
        workOrderId = UuidCreatorUtils.generateUuidV7();
        warehouseId = UuidCreatorUtils.generateUuidV7();

        contract = Contract.builder()
                .id(contractId)
                .customerId(customerId)
                .contractNumber("CTR-2026-TEST-01")
                .status(Contract.ContractStatus.SUSPENDED)
                .build();
    }

    @Test
    @DisplayName("Deve gerar O.S. de Retirada automaticamente para contrato com fatura vencida há mais de 30 dias")
    void shouldGenerateRemovalOrdersForOverdueContracts() {
        Invoice overdueInvoice = Invoice.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .contractId(contractId)
                .customerId(customerId)
                .status(Invoice.InvoiceStatus.OVERDUE)
                .dueDate(LocalDate.now().minusDays(35))
                .amount(new BigDecimal("99.90"))
                .build();

        when(invoiceRepository.findAll()).thenReturn(List.of(overdueInvoice));
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(workOrderRepository.findByContractIdAndType(contractId, WorkOrder.WorkOrderType.RETIRADA))
                .thenReturn(List.of());
        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        List<WorkOrder> generated = workOrderRemovalService.generateRemovalOrdersForOverdueContracts(30);

        assertThat(generated).hasSize(1);
        WorkOrder wo = generated.get(0);
        assertThat(wo.getContractId()).isEqualTo(contractId);
        assertThat(wo.getType()).isEqualTo(WorkOrder.WorkOrderType.RETIRADA);
        assertThat(wo.getStatus()).isEqualTo(WorkOrder.WorkOrderStatus.PENDING_SCHEDULE);
        verify(workOrderRepository, times(1)).save(any(WorkOrder.class));
        verify(domainEventPublisher, times(1)).publish(argThat(evt -> "REMOVAL_ORDER_GENERATED".equals(evt.getEventType())));
    }

    @Test
    @DisplayName("Não deve gerar O.S. de Retirada se já existir outra O.S. de remoção em andamento ou agendada")
    void shouldNotGenerateRemovalIfAlreadyActive() {
        Invoice overdueInvoice = Invoice.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .contractId(contractId)
                .customerId(customerId)
                .status(Invoice.InvoiceStatus.OVERDUE)
                .dueDate(LocalDate.now().minusDays(35))
                .build();

        WorkOrder existingWo = WorkOrder.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .contractId(contractId)
                .type(WorkOrder.WorkOrderType.RETIRADA)
                .status(WorkOrder.WorkOrderStatus.SCHEDULED)
                .build();

        when(invoiceRepository.findAll()).thenReturn(List.of(overdueInvoice));
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(workOrderRepository.findByContractIdAndType(contractId, WorkOrder.WorkOrderType.RETIRADA))
                .thenReturn(List.of(existingWo));

        List<WorkOrder> generated = workOrderRemovalService.generateRemovalOrdersForOverdueContracts(30);

        assertThat(generated).isEmpty();
        verify(workOrderRepository, never()).save(any(WorkOrder.class));
    }

    @Test
    @DisplayName("Deve concluir O.S. de Retirada com SUCESSO, devolver ONT ao depósito e cancelar contrato")
    void shouldCompleteSuccessfulRemoval() {
        WorkOrder wo = WorkOrder.builder()
                .id(workOrderId)
                .contractId(contractId)
                .customerId(customerId)
                .type(WorkOrder.WorkOrderType.RETIRADA)
                .status(WorkOrder.WorkOrderStatus.SCHEDULED)
                .build();

        SerializedAsset asset = SerializedAsset.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .serialNumber("HWTC-TEST-001")
                .replacementValue(new BigDecimal("420.00"))
                .currentContractId(contractId)
                .currentCustomerId(customerId)
                .status(SerializedAsset.AssetStatus.INSTALADO_CLIENTE)
                .build();

        when(workOrderRepository.findById(workOrderId)).thenReturn(Optional.of(wo));
        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        when(serializedAssetRepository.findByCurrentContractId(contractId)).thenReturn(List.of(asset));
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));

        WorkOrder result = workOrderRemovalService.completeSuccessfulRemoval(
                workOrderId, warehouseId, "https://storage.isperp.dev/photos/removal1.jpg", "Equipamento perfeito"
        );

        assertThat(result.getStatus()).isEqualTo(WorkOrder.WorkOrderStatus.COMPLETED);
        assertThat(result.getCompletedAt()).isNotNull();
        assertThat(result.getInstallationPhotoUrl()).isEqualTo("https://storage.isperp.dev/photos/removal1.jpg");

        // Verifica devolução via AssetCustodyService
        verify(assetCustodyService, times(1)).returnAssetFromWorkOrder(
                eq(asset.getId()), eq(warehouseId), eq(false), eq("https://storage.isperp.dev/photos/removal1.jpg"), anyString()
        );

        // Verifica que o contrato foi cancelado
        assertThat(contract.getStatus()).isEqualTo(Contract.ContractStatus.CANCELED);
        verify(contractRepository, times(1)).save(contract);

        // Verifica emissão do evento de remoção concluída
        verify(domainEventPublisher, times(1)).publish(argThat(evt -> "REMOVAL_ORDER_COMPLETED".equals(evt.getEventType())));
    }

    @Test
    @DisplayName("Deve registrar O.S. como INFRUTÍFERA, cancelar contrato e gerar processo de cobrança jurídica / SPC")
    void shouldCompleteUnsuccessfulRemovalAndCreateLegalCollection() {
        WorkOrder wo = WorkOrder.builder()
                .id(workOrderId)
                .contractId(contractId)
                .customerId(customerId)
                .type(WorkOrder.WorkOrderType.RETIRADA)
                .status(WorkOrder.WorkOrderStatus.SCHEDULED)
                .build();

        Invoice inv1 = Invoice.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .contractId(contractId)
                .amount(new BigDecimal("100.00"))
                .status(Invoice.InvoiceStatus.OVERDUE)
                .build();

        Invoice inv2 = Invoice.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .contractId(contractId)
                .amount(new BigDecimal("100.00"))
                .status(Invoice.InvoiceStatus.OVERDUE)
                .build();

        SerializedAsset asset = SerializedAsset.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .serialNumber("HWTC-LOST-001")
                .replacementValue(new BigDecimal("420.00"))
                .currentContractId(contractId)
                .currentCustomerId(customerId)
                .status(SerializedAsset.AssetStatus.INSTALADO_CLIENTE)
                .build();

        when(workOrderRepository.findById(workOrderId)).thenReturn(Optional.of(wo));
        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(invoiceRepository.findByContractId(contractId)).thenReturn(List.of(inv1, inv2));
        when(serializedAssetRepository.findByCurrentContractId(contractId)).thenReturn(List.of(asset));
        when(legalCollectionRepository.save(any(LegalCollectionRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        LegalCollectionRecord record = workOrderRemovalService.completeUnsuccessfulRemoval(
                workOrderId, "CLIENTE_RECUSOU_ENTREGA", null, "Cliente agrediu verbalmente o técnico e não entregou a ONT"
        );

        // O.S. deve estar INFRUTIFERA
        assertThat(wo.getStatus()).isEqualTo(WorkOrder.WorkOrderStatus.INFRUTIFERA);
        assertThat(wo.getUnsuccessReason()).isEqualTo("CLIENTE_RECUSOU_ENTREGA");

        // Contrato cancelado
        assertThat(contract.getStatus()).isEqualTo(Contract.ContractStatus.CANCELED);
        verify(contractRepository, times(1)).save(contract);

        // Processo Jurídico / SPC gerado com dívida consolidada
        assertThat(record).isNotNull();
        assertThat(record.getCustomerId()).isEqualTo(customerId);
        assertThat(record.getContractId()).isEqualTo(contractId);
        assertThat(record.getWorkOrderId()).isEqualTo(workOrderId);
        assertThat(record.getTotalDebtInvoices()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(record.getEquipmentIndemnityAmount()).isEqualByComparingTo(new BigDecimal("420.00"));
        assertThat(record.getTotalClaimAmount()).isEqualByComparingTo(new BigDecimal("620.00"));
        assertThat(record.getStatus()).isEqualTo(LegalCollectionRecord.LegalCollectionStatus.PENDING_BUREAU_SUBMISSION);
        assertThat(record.getEvidenceNotes()).contains("CLIENTE_RECUSOU_ENTREGA");

        verify(legalCollectionRepository, times(1)).save(any(LegalCollectionRecord.class));

        // Verifica emissão do evento de cobrança jurídica/SPC
        verify(domainEventPublisher, times(1)).publish(argThat(evt -> "LEGAL_COLLECTION_RECORD_CREATED".equals(evt.getEventType())));
    }
}
