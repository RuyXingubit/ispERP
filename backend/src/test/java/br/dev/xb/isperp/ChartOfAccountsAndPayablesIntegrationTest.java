package br.dev.xb.isperp;

import br.dev.xb.isperp.dto.financial.*;
import br.dev.xb.isperp.entity.Customer;
import br.dev.xb.isperp.entity.User;
import br.dev.xb.isperp.entity.UserRole;
import br.dev.xb.isperp.entity.WorkOrder;
import br.dev.xb.isperp.entity.financial.ChartOfAccount;
import br.dev.xb.isperp.entity.financial.FeeStatus;
import br.dev.xb.isperp.entity.financial.PayableStatus;
import br.dev.xb.isperp.repository.CustomerRepository;
import br.dev.xb.isperp.repository.UserRepository;
import br.dev.xb.isperp.repository.WorkOrderRepository;
import br.dev.xb.isperp.repository.financial.ChartOfAccountRepository;
import br.dev.xb.isperp.repository.financial.ExpenseInstallmentRepository;
import br.dev.xb.isperp.repository.financial.PayableInvoiceRepository;
import br.dev.xb.isperp.service.financial.ChartOfAccountService;
import br.dev.xb.isperp.service.financial.PayableInvoiceService;
import br.dev.xb.isperp.service.financial.WorkOrderFeeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("null")
class ChartOfAccountsAndPayablesIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ChartOfAccountService chartOfAccountService;

    @Autowired
    private PayableInvoiceService payableInvoiceService;

    @Autowired
    private WorkOrderFeeService workOrderFeeService;

    @Autowired
    private ChartOfAccountRepository chartOfAccountRepository;

    @Autowired
    private PayableInvoiceRepository payableInvoiceRepository;

    @Autowired
    private ExpenseInstallmentRepository installmentRepository;

    @Autowired
    private WorkOrderRepository workOrderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private br.dev.xb.isperp.repository.PlanRepository planRepository;

    @Autowired
    private br.dev.xb.isperp.repository.ContractRepository contractRepository;

    @Test
    @DisplayName("Valida no PostgreSQL 17 real: Seeder de 5 Níveis ➔ Contas a Pagar com Parcelamento ➔ Esteira de Isenção de Taxa de O.S.")
    void shouldVerifyChartOfAccountsPayablesAndFeeWaiversOnPostgres17() {
        // 1. Validar que o Seeder V28 foi executado pelo Flyway no Postgres 17 real
        List<ChartOfAccountDto> tree = chartOfAccountService.getTree();
        assertFalse(tree.isEmpty(), "A árvore de contas deve conter os grupos canônicos seedados");

        // Verifica existência dos grupos principais
        assertTrue(chartOfAccountRepository.findByCode("01").isPresent(), "Grupo 01. RECEITAS");
        assertTrue(chartOfAccountRepository.findByCode("02").isPresent(), "Grupo 02. IMPOSTOS");
        assertTrue(chartOfAccountRepository.findByCode("03").isPresent(), "Grupo 03. INTERCONEXÃO");
        assertTrue(chartOfAccountRepository.findByCode("04").isPresent(), "Grupo 04. OPEX / POSTES");
        assertTrue(chartOfAccountRepository.findByCode("05").isPresent(), "Grupo 05. CAPEX / MÁQUINAS");

        // Conta analítica de cabos ópticos (05.01.01)
        ChartOfAccount dropAccount = chartOfAccountRepository.findByCode("05.01.01")
                .orElseThrow(() -> new AssertionError("Conta 05.01.01 de bobinas de drop não encontrada no banco real"));

        // 2. Criar uma Conta a Pagar com 3 parcelas de R$ 2.000,00 (Total R$ 6.000,00 em CAPEX)
        PayableInvoiceRequest payableRequest = PayableInvoiceRequest.builder()
                .supplierName("Distribuidora de Fibra Óptica Brasil")
                .supplierDocument("98.765.432/0001-10")
                .chartOfAccountId(dropAccount.getId())
                .description("Lote de 20 bobinas de drop 1km para expansão")
                .totalAmount(new BigDecimal("6000.00"))
                .installmentsCount(3)
                .firstDueDate(LocalDate.now().plusMonths(1))
                .notes("Pedido de compra aprovado pela diretoria")
                .build();

        PayableInvoiceDto createdPayable = payableInvoiceService.createPayableInvoice(payableRequest);
        assertThat(createdPayable.getId()).isNotNull();
        assertThat(createdPayable.getInstallments()).hasSize(3);

        // Valida no banco de dados real que as parcelas foram persistidas
        var installmentsInDb = installmentRepository.findByPayableInvoiceIdOrderByInstallmentNumberAsc(createdPayable.getId());
        assertThat(installmentsInDb).hasSize(3);
        assertThat(installmentsInDb.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("2000.00"));
        assertThat(installmentsInDb.get(1).getAmount()).isEqualByComparingTo(new BigDecimal("2000.00"));
        assertThat(installmentsInDb.get(2).getAmount()).isEqualByComparingTo(new BigDecimal("2000.00"));

        // 3. Quitar a 1ª parcela de R$ 2.000,00 no banco real
        UUID firstInstallmentId = installmentsInDb.get(0).getId();
        ExpenseInstallmentDto paidInstallment = payableInvoiceService.payInstallment(
                firstInstallmentId,
                new BigDecimal("2000.00"),
                "TED_BANCARIA",
                "https://storage.isperp.dev/receipts/comprovante_ted_2000.pdf"
        );

        assertThat(paidInstallment.getStatus()).isEqualTo(PayableStatus.PAID);
        var updatedPayableInDb = payableInvoiceRepository.findById(createdPayable.getId()).orElseThrow();
        assertThat(updatedPayableInDb.getStatus()).isEqualTo(PayableStatus.PARTIALLY_PAID);

        // 4. Testar a Esteira de Isenção de Taxa de O.S. (Mudança de Endereço)
        User attendant = userRepository.save(User.builder()
                .name("Atendente Suporte Real")
                .email("atendente.real." + System.currentTimeMillis() + "@isperp.com.br")
                .password("hash123456")
                .cpf(generateValidCpf())
                .role(UserRole.ATTENDANT)
                .build());

        User cfo = userRepository.save(User.builder()
                .name("Roberto CFO Gestor")
                .email("cfo.gestor." + System.currentTimeMillis() + "@isperp.com.br")
                .password("hash123456")
                .cpf(generateValidCpf())
                .role(UserRole.CFO)
                .build());

        Customer customer = customerRepository.save(Customer.builder()
                .name("Maria Cliente Antiga")
                .cpf(generateValidCpf())
                .email("maria.antiga." + System.currentTimeMillis() + "@gmail.com")
                .phone("11977776666")
                .build());

        br.dev.xb.isperp.entity.Plan plan = planRepository.save(br.dev.xb.isperp.entity.Plan.builder()
                .name("Plano 500 Mega Real")
                .downloadSpeed(500)
                .uploadSpeed(250)
                .price(new BigDecimal("99.90"))
                .build());

        br.dev.xb.isperp.entity.Contract contract = contractRepository.save(br.dev.xb.isperp.entity.Contract.builder()
                .customerId(customer.getId())
                .planId(plan.getId())
                .contractNumber("CTR-ISENCAO-" + System.currentTimeMillis())
                .monthlyFee(new BigDecimal("99.90"))
                .dueDay(10)
                .installationAddress("Rua das Oliveiras, 45")
                .city("Santarém")
                .state("PA")
                .zipCode("68000-000")
                .status(br.dev.xb.isperp.entity.Contract.ContractStatus.ACTIVE)
                .build());

        WorkOrder workOrder = workOrderRepository.save(WorkOrder.builder()
                .customerId(customer.getId())
                .contractId(contract.getId())
                .standardFeeAmount(new BigDecimal("100.00"))
                .feeStatus(FeeStatus.BILLABLE)
                .build());

        // Atendente solicita isenção
        WorkOrderFeeWaiverRequest waiverRequest = WorkOrderFeeWaiverRequest.builder()
                .workOrderId(workOrder.getId())
                .waiverReason("Cliente há 4 anos com alto LTV. Reclamou da taxa e ameaçou cancelamento.")
                .build();

        WorkOrderFeeDto requestedWaiver = workOrderFeeService.requestWaiver(attendant.getId(), waiverRequest);
        assertThat(requestedWaiver.getFeeStatus()).isEqualTo(FeeStatus.PENDING_WAIVER_APPROVAL);

        // CFO audita e aprova
        WorkOrderFeeAuditRequest auditRequest = WorkOrderFeeAuditRequest.builder()
                .approved(true)
                .notes("Aprovado pelo CFO para retenção de cliente estratégico.")
                .build();

        WorkOrderFeeDto approvedWaiver = workOrderFeeService.auditWaiver(cfo.getId(), workOrder.getId(), auditRequest);
        assertThat(approvedWaiver.getFeeStatus()).isEqualTo(FeeStatus.WAIVED_APPROVED);

        // Valida persistência da aprovação no PostgreSQL 17 real
        WorkOrder auditedInDb = workOrderRepository.findById(workOrder.getId()).orElseThrow();
        assertThat(auditedInDb.getFeeStatus()).isEqualTo(FeeStatus.WAIVED_APPROVED);
        assertThat(auditedInDb.getWaiverAuditedByUserId()).isEqualTo(cfo.getId());
        assertThat(auditedInDb.getWaiverAuditedAt()).isNotNull();
    }

    private String generateValidCpf() {
        int[] digits = new int[11];
        java.util.Random rnd = new java.util.Random();
        for (int i = 0; i < 9; i++) {
            digits[i] = rnd.nextInt(10);
        }
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += digits[i] * (10 - i);
        }
        int rem = sum % 11;
        digits[9] = rem < 2 ? 0 : 11 - rem;
        sum = 0;
        for (int i = 0; i < 10; i++) {
            sum += digits[i] * (11 - i);
        }
        rem = sum % 11;
        digits[10] = rem < 2 ? 0 : 11 - rem;
        StringBuilder sb = new StringBuilder();
        for (int d : digits) {
            sb.append(d);
        }
        return sb.toString();
    }
}
