package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.*;
import br.dev.xb.isperp.entity.*;
import br.dev.xb.isperp.mapper.RadiusLifecycleMapper;
import br.dev.xb.isperp.radius.NasVendorType;
import br.dev.xb.isperp.radius.RadiusBlockMode;
import br.dev.xb.isperp.radius.RadiusLifecycleActionType;
import br.dev.xb.isperp.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RadiusLifecycleServiceTest {

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private OnuProvisioningRepository onuProvisioningRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private RadiusPolicyConfigRepository policyConfigRepository;

    @Mock
    private RadiusLifecycleLogRepository lifecycleLogRepository;

    @Mock
    private RadiusProvisioningService radiusProvisioningService;

    @Mock
    private RadiusSessionService radiusSessionService;

    @Mock
    private NasRepository nasRepository;

    private RadiusLifecycleMapper lifecycleMapper = Mappers.getMapper(RadiusLifecycleMapper.class);

    private RadiusLifecycleService lifecycleService;

    private UUID contractId;
    private UUID customerId;
    private UUID planId;
    private Contract contract;
    private Customer customer;
    private Plan plan;
    private OnuProvisioning onu;
    private RadiusPolicyConfig policyConfig;

    @BeforeEach
    void setUp() {
        lifecycleService = new RadiusLifecycleService(
                contractRepository,
                customerRepository,
                planRepository,
                onuProvisioningRepository,
                invoiceRepository,
                policyConfigRepository,
                lifecycleLogRepository,
                radiusProvisioningService,
                radiusSessionService,
                nasRepository,
                lifecycleMapper
        );

        contractId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        planId = UUID.randomUUID();

        customer = Customer.builder()
                .id(customerId)
                .name("Carlos Alberto")
                .cpf("12345678900")
                .build();

        plan = Plan.builder()
                .id(planId)
                .name("Fibra 500M")
                .downloadSpeed(500)
                .uploadSpeed(250)
                .build();

        contract = Contract.builder()
                .id(contractId)
                .contractNumber("CTR-2026-001")
                .customerId(customerId)
                .planId(planId)
                .status(Contract.ContractStatus.ACTIVE)
                .build();

        onu = OnuProvisioning.builder()
                .id(UUID.randomUUID())
                .contractId(contractId)
                .customerId(customerId)
                .pppoeUser("carlos_pppoe")
                .pppoePassword("secret123")
                .downloadSpeed(500)
                .uploadSpeed(250)
                .status(OnuProvisioning.OnuStatus.PROVISIONED)
                .build();

        policyConfig = RadiusPolicyConfig.builder()
                .id(UUID.randomUUID())
                .autoBlockEnabled(true)
                .toleranceDays(5)
                .blockMode(RadiusBlockMode.CAPTIVE_PORTAL)
                .sendPodOnBlock(true)
                .sendPodOnUnblock(true)
                .unblockOnPayment(true)
                .build();
    }

    @Test
    @DisplayName("Deve sincronizar contrato ativo no FreeRADIUS com sucesso")
    void testSyncContractToRadius() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(onuProvisioningRepository.findByContractId(contractId)).thenReturn(Optional.of(onu));

        lifecycleService.syncContractToRadius(contractId);

        verify(radiusProvisioningService).provisionUser(
                eq("carlos_pppoe"),
                eq("secret123"),
                eq(500L),
                eq(250L),
                eq(NasVendorType.MIKROTIK),
                isNull(),
                isNull()
        );
        verify(lifecycleLogRepository).save(any(RadiusLifecycleLog.class));
    }

    @Test
    @DisplayName("Deve executar auto-corte por inadimplência e enviar pacote PoD")
    void testExecuteAutoBlock() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(onuProvisioningRepository.findByContractId(contractId)).thenReturn(Optional.of(onu));
        when(policyConfigRepository.findFirstConfig()).thenReturn(Optional.of(policyConfig));
        when(radiusSessionService.disconnectUser(any())).thenReturn(RadiusDisconnectResponse.builder().success(true).build());

        lifecycleService.executeAutoBlock(contractId, "Fatura vencida há 7 dias");

        assertThat(contract.getStatus()).isEqualTo(Contract.ContractStatus.SUSPENDED);
        assertThat(onu.getStatus()).isEqualTo(OnuProvisioning.OnuStatus.BLOCKED);

        verify(radiusProvisioningService).blockUser(eq("carlos_pppoe"), eq(NasVendorType.MIKROTIK), eq("Fatura vencida há 7 dias"));
        verify(radiusSessionService).disconnectUser(any());
        verify(contractRepository).save(contract);
        verify(onuProvisioningRepository).save(onu);
        verify(lifecycleLogRepository).save(any(RadiusLifecycleLog.class));
    }

    @Test
    @DisplayName("Deve executar desbloqueio instantâneo e restaurar velocidade com PoD")
    void testExecuteInstantUnblock() {
        contract.setStatus(Contract.ContractStatus.SUSPENDED);
        onu.setStatus(OnuProvisioning.OnuStatus.BLOCKED);

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(onuProvisioningRepository.findByContractId(contractId)).thenReturn(Optional.of(onu));
        when(policyConfigRepository.findFirstConfig()).thenReturn(Optional.of(policyConfig));
        when(radiusSessionService.disconnectUser(any())).thenReturn(RadiusDisconnectResponse.builder().success(true).build());

        lifecycleService.executeInstantUnblock(contractId, "Pagamento PIX confirmado", RadiusLifecycleActionType.PAYMENT_UNBLOCK);

        assertThat(contract.getStatus()).isEqualTo(Contract.ContractStatus.ACTIVE);
        assertThat(onu.getStatus()).isEqualTo(OnuProvisioning.OnuStatus.PROVISIONED);

        verify(radiusProvisioningService).unblockUser(
                eq("carlos_pppoe"),
                eq(500L),
                eq(250L),
                eq(NasVendorType.MIKROTIK),
                isNull(),
                isNull()
        );
        verify(radiusSessionService).disconnectUser(any());
        verify(contractRepository).save(contract);
        verify(onuProvisioningRepository).save(onu);
        verify(lifecycleLogRepository).save(any(RadiusLifecycleLog.class));
    }

    @Test
    @DisplayName("Deve verificar elegibilidade de cliente sem faturas vencidas além da tolerância")
    void testIsCustomerEligibleForUnblock() {
        when(policyConfigRepository.findFirstConfig()).thenReturn(Optional.of(policyConfig));

        // Fatura vencida ontem (dentro dos 5 dias de tolerância)
        Invoice recentOverdue = Invoice.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .dueDate(LocalDate.now().minusDays(1))
                .status(Invoice.InvoiceStatus.PENDING)
                .build();

        when(invoiceRepository.findByCustomerIdAndStatus(customerId, Invoice.InvoiceStatus.PENDING))
                .thenReturn(List.of(recentOverdue));

        boolean eligible = lifecycleService.isCustomerEligibleForUnblock(customerId);
        assertThat(eligible).isTrue();
    }
}
