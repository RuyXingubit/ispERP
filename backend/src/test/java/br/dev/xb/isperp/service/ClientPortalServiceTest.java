package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.ClientPortalDashboardDTO;
import br.dev.xb.isperp.dto.UpdateClientProfileRequest;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class ClientPortalServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private TrustUnblockRepository trustUnblockRepository;

    @Mock
    private PlanUpgradeRequestRepository planUpgradeRequestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ClientPortalService clientPortalService;

    private UUID customerId;
    private UUID contractId;
    private UUID planId;
    private Customer customer;
    private Contract contract;
    private Plan plan;

    @BeforeEach
    void setUp() {
        customerId = UuidCreatorUtils.generateUuidV7();
        contractId = UuidCreatorUtils.generateUuidV7();
        planId = UuidCreatorUtils.generateUuidV7();

        customer = Customer.builder()
                .id(customerId)
                .name("Ruy Cliente")
                .cpf("52998224725")
                .email("cliente@xingubit.com.br")
                .phone("11988887777")
                .build();

        plan = Plan.builder()
                .id(planId)
                .name("Fibra 500 Mega")
                .downloadSpeed(500)
                .uploadSpeed(250)
                .price(new BigDecimal("99.90"))
                .build();

        contract = Contract.builder()
                .id(contractId)
                .customerId(customerId)
                .planId(planId)
                .contractNumber("CTR-2026-001")
                .status(Contract.ContractStatus.ACTIVE)
                .monthlyFee(new BigDecimal("99.90"))
                .dueDay(10)
                .installationAddress("Rua Exemplo, 123")
                .city("São Paulo")
                .state("SP")
                .zipCode("01310-100")
                .build();
    }

    @Test
    @DisplayName("Deve carregar o dashboard completo do assinante")
    void shouldGetClientDashboard() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(contractRepository.findByCustomerId(customerId)).thenReturn(List.of(contract));
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(planRepository.findByActiveTrue()).thenReturn(List.of(plan));

        Invoice pendingInvoice = Invoice.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .contractId(contractId)
                .customerId(customerId)
                .amount(new BigDecimal("99.90"))
                .dueDate(LocalDate.now().plusDays(5))
                .status(Invoice.InvoiceStatus.PENDING)
                .build();

        when(invoiceRepository.findByCustomerIdOrderByDueDateDesc(customerId)).thenReturn(List.of(pendingInvoice));

        ClientPortalDashboardDTO dashboard = clientPortalService.getClientDashboard(customerId);

        assertNotNull(dashboard);
        assertEquals(customer.getName(), dashboard.getCustomer().getName());
        assertEquals("Fibra 500 Mega", dashboard.getCurrentPlan().getName());
        assertEquals(1, dashboard.getPendingInvoices().size());
        assertFalse(dashboard.isConnectionBlocked());
    }

    @Test
    @DisplayName("Deve atualizar dados cadastrais do cliente")
    void shouldUpdateClientProfile() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateClientProfileRequest req = UpdateClientProfileRequest.builder()
                .name("Ruy Atualizado")
                .email("ruy.novo@xingubit.com.br")
                .phone("11977776666")
                .city("Campinas")
                .build();

        Customer updated = clientPortalService.updateProfile(customerId, req);

        assertEquals("Ruy Atualizado", updated.getName());
        assertEquals("ruy.novo@xingubit.com.br", updated.getEmail());
        assertEquals("11977776666", updated.getPhone());
    }

    @Test
    @DisplayName("Deve realizar upgrade de plano e disparar evento PLAN_UPGRADED")
    void shouldRequestPlanUpgrade() {
        UUID newPlanId = UuidCreatorUtils.generateUuidV7();
        Plan newPlan = Plan.builder()
                .id(newPlanId)
                .name("Fibra 1 Giga")
                .downloadSpeed(1000)
                .uploadSpeed(500)
                .price(new BigDecimal("149.90"))
                .build();

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(planRepository.findById(newPlanId)).thenReturn(Optional.of(newPlan));
        when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Contract result = clientPortalService.requestPlanUpgrade(customerId, contractId, newPlanId);

        assertEquals(newPlanId, result.getPlanId());
        assertEquals(new BigDecimal("149.90"), result.getMonthlyFee());
        verify(outboxEventRepository).save(any(OutboxEvent.class));
    }

    @Test
    @DisplayName("Deve conceder desbloqueio em confiança de 48h para contrato bloqueado")
    void shouldRequestTrustUnblock() {
        contract.setStatus(Contract.ContractStatus.SUSPENDED);
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(trustUnblockRepository.findFirstByContractIdAndStatusOrderByRequestedAtDesc(contractId, "ACTIVE"))
                .thenReturn(Optional.empty());
        when(trustUnblockRepository.save(any(TrustUnblock.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TrustUnblock unblock = clientPortalService.requestTrustUnblock(customerId, contractId);

        assertNotNull(unblock);
        assertEquals("ACTIVE", unblock.getStatus());
        assertEquals(Contract.ContractStatus.ACTIVE, contract.getStatus());
        verify(outboxEventRepository).save(any(OutboxEvent.class));
    }
}
