package br.dev.xb.isperp.service;

import br.dev.xb.isperp.api.dto.ContractAddressChangeRequest;
import br.dev.xb.isperp.api.dto.ContractOwnershipTransferRequest;
import br.dev.xb.isperp.api.dto.ContractPlanChangeRequest;
import br.dev.xb.isperp.dto.SignatureSessionResponse;
import br.dev.xb.isperp.entity.*;
import br.dev.xb.isperp.entity.financial.FeeStatus;
import br.dev.xb.isperp.repository.*;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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
    @Mock
    private PlanRepository planRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private WorkOrderRepository workOrderRepository;
    @Mock
    private ContractLifecycleEventRepository lifecycleEventRepository;
    @Mock
    private RadiusLifecycleService radiusLifecycleService;
    @Mock
    private ElectronicSignatureService electronicSignatureService;

    private ContractService contractService;

    private Contract sampleContract;
    private UUID contractId;

    @BeforeEach
    void setUp() {
        contractService = new ContractService(
                contractRepository,
                planRepository,
                customerRepository,
                invoiceRepository,
                workOrderRepository,
                lifecycleEventRepository,
                radiusLifecycleService,
                electronicSignatureService
        );
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

    @Test
    @DisplayName("Onda I: Deve realizar Upgrade de Plano com Pro-Rata Die ajustando fatura PENDING e sincronizando FreeRADIUS")
    void shouldChangePlanWithProRataAndRadiusSync() {
        UUID newPlanId = UuidCreatorUtils.generateUuidV7();
        Plan newPlan = Plan.builder()
                .id(newPlanId)
                .name("Fibra 800 Mega")
                .downloadSpeed(800)
                .uploadSpeed(400)
                .price(new BigDecimal("129.90"))
                .build();

        UUID openInvoiceId = UuidCreatorUtils.generateUuidV7();
        Invoice openInvoice = Invoice.builder()
                .id(openInvoiceId)
                .contractId(contractId)
                .customerId(sampleContract.getCustomerId())
                .amount(new BigDecimal("99.90"))
                .dueDate(LocalDate.now().plusDays(15))
                .status(Invoice.InvoiceStatus.PENDING)
                .compositionItems(new ArrayList<>(List.of(
                        InvoiceCompositionItem.builder()
                                .id(UuidCreatorUtils.generateUuidV7())
                                .name("Acesso SCM")
                                .category(PlanCompositionCategory.SCM)
                                .amount(new BigDecimal("99.90"))
                                .build()
                )))
                .build();

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(sampleContract));
        when(planRepository.findById(newPlanId)).thenReturn(Optional.of(newPlan));
        when(invoiceRepository.findByContractIdOrderByDueDateDesc(contractId)).thenReturn(List.of(openInvoice));
        when(contractRepository.save(any(Contract.class))).thenAnswer(i -> i.getArgument(0));
        when(lifecycleEventRepository.save(any(ContractLifecycleEvent.class))).thenAnswer(i -> i.getArgument(0));

        ContractPlanChangeRequest req = new ContractPlanChangeRequest()
                .newPlanId(newPlanId)
                .applyProRata(true)
                .reason("Upgrade comercial 800M");

        ContractService.LifecycleActionResult result = contractService.changePlan(contractId, req);

        assertEquals(newPlanId, result.contract().getPlanId());
        assertEquals(new BigDecimal("129.90"), result.contract().getMonthlyFee());
        assertEquals(ContractLifecycleEvent.EventType.PLAN_CHANGE, result.event().getEventType());
        assertEquals(openInvoiceId, result.event().getAdjustedInvoiceId());
        assertEquals("800M/400M", result.event().getRadiusRateLimitApplied());
        assertTrue(result.event().getProRataAdjustmentAmount().compareTo(BigDecimal.ZERO) > 0);
        verify(radiusLifecycleService, times(1)).syncContractToRadius(contractId);
        verify(invoiceRepository, times(1)).save(openInvoice);
    }

    @Test
    @DisplayName("Onda I: Deve alterar endereço do contrato e abrir O.S. com solicitação de isenção de taxa")
    void shouldChangeAddressAndOpenWorkOrderWithWaiver() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(sampleContract));
        when(contractRepository.save(any(Contract.class))).thenAnswer(i -> i.getArgument(0));
        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(i -> i.getArgument(0));
        when(lifecycleEventRepository.save(any(ContractLifecycleEvent.class))).thenAnswer(i -> i.getArgument(0));

        ContractAddressChangeRequest req = new ContractAddressChangeRequest()
                .newInstallationAddress("Av. Brasil, 900")
                .newCity("Altamira")
                .newState("PA")
                .newZipCode("68371-000")
                .serviceFee(150.00)
                .requestWaiver(true)
                .waiverReason("Fidelidade renovada 12 meses");

        ContractService.LifecycleActionResult result = contractService.changeAddress(contractId, req);

        assertEquals("Av. Brasil, 900", result.contract().getInstallationAddress());
        assertEquals(ContractLifecycleEvent.EventType.ADDRESS_CHANGE, result.event().getEventType());
        assertNotNull(result.event().getWorkOrderId());
        verify(workOrderRepository, times(1)).save(argThat(wo ->
                wo.getFeeStatus() == FeeStatus.PENDING_WAIVER_APPROVAL
                        && new BigDecimal("150.00").compareTo(wo.getStandardFeeAmount()) == 0));
    }

    @Test
    @DisplayName("Onda I: Deve transferir titularidade migrando faturas PENDING e OVERDUE e gerando nova Assinatura Forense")
    void shouldTransferOwnershipAndMigrateInvoices() {
        UUID newCustomerId = UuidCreatorUtils.generateUuidV7();
        Customer newCustomer = Customer.builder()
                .id(newCustomerId)
                .name("Novo Titular")
                .cpf("12345678909")
                .build();

        Invoice overdueInv = Invoice.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .contractId(contractId)
                .customerId(sampleContract.getCustomerId())
                .amount(new BigDecimal("99.90"))
                .dueDate(LocalDate.now().minusDays(10))
                .status(Invoice.InvoiceStatus.OVERDUE)
                .build();

        Invoice pendingInv = Invoice.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .contractId(contractId)
                .customerId(sampleContract.getCustomerId())
                .amount(new BigDecimal("99.90"))
                .dueDate(LocalDate.now().plusDays(10))
                .status(Invoice.InvoiceStatus.PENDING)
                .build();

        Invoice paidInv = Invoice.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .contractId(contractId)
                .customerId(sampleContract.getCustomerId())
                .amount(new BigDecimal("99.90"))
                .dueDate(LocalDate.now().minusDays(40))
                .status(Invoice.InvoiceStatus.PAID)
                .build();

        UUID sigSessionId = UuidCreatorUtils.generateUuidV7();
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(sampleContract));
        when(customerRepository.findById(newCustomerId)).thenReturn(Optional.of(newCustomer));
        when(invoiceRepository.findByContractIdOrderByDueDateDesc(contractId))
                .thenReturn(List.of(pendingInv, overdueInv, paidInv));
        when(contractRepository.save(any(Contract.class))).thenAnswer(i -> i.getArgument(0));
        when(electronicSignatureService.createSignatureSession(any(), any()))
                .thenReturn(SignatureSessionResponse.builder()
                        .id(sigSessionId)
                        .signatureUrl("https://cliente.provedor.com.br/sign/token123")
                        .build());
        when(lifecycleEventRepository.save(any(ContractLifecycleEvent.class))).thenAnswer(i -> i.getArgument(0));

        ContractOwnershipTransferRequest req = new ContractOwnershipTransferRequest()
                .newCustomerId(newCustomerId)
                .generateForensicSignature(true)
                .reason("Assunção contratual e financeira pelo novo morador");

        ContractService.LifecycleActionResult result = contractService.transferOwnership(contractId, req);

        assertEquals(newCustomerId, result.contract().getCustomerId());
        assertEquals(2, result.event().getMigratedInvoicesCount());
        assertEquals(newCustomerId, overdueInv.getCustomerId());
        assertEquals(newCustomerId, pendingInv.getCustomerId());
        assertNotEquals(newCustomerId, paidInv.getCustomerId());
        assertEquals(sigSessionId, result.event().getSignatureSessionId());
        assertEquals("https://cliente.provedor.com.br/sign/token123", result.event().getSignatureUrl());
    }
}
