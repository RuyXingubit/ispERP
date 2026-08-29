package br.dev.xb.isperp.service;

import br.dev.xb.isperp.entity.Contract;
import br.dev.xb.isperp.entity.Customer;
import br.dev.xb.isperp.entity.Invoice;
import br.dev.xb.isperp.entity.Plan;
import br.dev.xb.isperp.repository.ContractRepository;
import br.dev.xb.isperp.repository.CustomerRepository;
import br.dev.xb.isperp.repository.PlanRepository;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class NfcomDecisionServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private PlanRepository planRepository;

    @InjectMocks
    private NfcomDecisionService nfcomDecisionService;

    private UUID customerId;
    private UUID contractId;
    private UUID planId;
    private Invoice invoice;

    @BeforeEach
    void setUp() {
        customerId = UuidCreatorUtils.generateUuidV7();
        contractId = UuidCreatorUtils.generateUuidV7();
        planId = UuidCreatorUtils.generateUuidV7();

        invoice = Invoice.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .customerId(customerId)
                .contractId(contractId)
                .build();
    }

    @Test
    @DisplayName("Deve emitir NFCom quando configurado explicitamente no Cliente")
    void shouldIssueWhenCustomerConfiguredTrue() {
        Customer customer = Customer.builder()
                .id(customerId)
                .alwaysIssueNfcom(true)
                .build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        boolean shouldIssue = nfcomDecisionService.shouldIssueNfcom(invoice);

        assertTrue(shouldIssue);
    }

    @Test
    @DisplayName("Não deve emitir NFCom quando Cliente configurado como false, mesmo que Plano seja true")
    void shouldNotIssueWhenCustomerConfiguredFalseOverride() {
        Customer customer = Customer.builder()
                .id(customerId)
                .alwaysIssueNfcom(false)
                .build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        boolean shouldIssue = nfcomDecisionService.shouldIssueNfcom(invoice);

        assertFalse(shouldIssue);
    }

    @Test
    @DisplayName("Deve emitir NFCom herdando do Plano quando Cliente não tem configuração explícita")
    void shouldInheritFromPlanWhenCustomerNull() {
        Customer customer = Customer.builder()
                .id(customerId)
                .alwaysIssueNfcom(null)
                .build();

        Contract contract = Contract.builder()
                .id(contractId)
                .planId(planId)
                .build();

        Plan plan = Plan.builder()
                .id(planId)
                .alwaysIssueNfcom(true)
                .build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));

        boolean shouldIssue = nfcomDecisionService.shouldIssueNfcom(invoice);

        assertTrue(shouldIssue);
    }

    @Test
    @DisplayName("Padrão: Não deve emitir NFCom quando nem Cliente nem Plano estão configurados")
    void shouldDefaultToFalseWhenNoConfiguration() {
        Customer customer = Customer.builder()
                .id(customerId)
                .alwaysIssueNfcom(null)
                .build();

        Contract contract = Contract.builder()
                .id(contractId)
                .planId(planId)
                .build();

        Plan plan = Plan.builder()
                .id(planId)
                .alwaysIssueNfcom(false)
                .build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));

        boolean shouldIssue = nfcomDecisionService.shouldIssueNfcom(invoice);

        assertFalse(shouldIssue);
    }
}
