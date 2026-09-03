package br.dev.xb.isperp;

import br.dev.xb.isperp.dto.financial.*;
import br.dev.xb.isperp.entity.Contract;
import br.dev.xb.isperp.entity.Customer;
import br.dev.xb.isperp.entity.Invoice;
import br.dev.xb.isperp.entity.Plan;
import br.dev.xb.isperp.entity.financial.ChartOfAccount;
import br.dev.xb.isperp.repository.ContractRepository;
import br.dev.xb.isperp.repository.CustomerRepository;
import br.dev.xb.isperp.repository.InvoiceRepository;
import br.dev.xb.isperp.repository.PlanRepository;
import br.dev.xb.isperp.repository.financial.ChartOfAccountRepository;
import br.dev.xb.isperp.service.financial.DeleveragingEngineService;
import br.dev.xb.isperp.service.financial.DreReportService;
import br.dev.xb.isperp.service.financial.PayableInvoiceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("null")
class DeleveragingAndDreIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private DreReportService dreReportService;

    @Autowired
    private DeleveragingEngineService deleveragingEngineService;

    @Autowired
    private PayableInvoiceService payableInvoiceService;

    @Autowired
    private ChartOfAccountRepository chartOfAccountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Test
    @DisplayName("Valida no PostgreSQL 17 real: DRE Telecom (EBITDA), Curva de Desalavancagem 36M e Simulador E Se")
    void shouldVerifyDreAndDeleveragingOnPostgres17() {
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());

        // 1. Criar Cliente e Contrato Ativo no banco real
        Customer customer = customerRepository.save(Customer.builder()
                .name("Provedor Cliente Base Real")
                .cpf(generateValidCpf())
                .email("cliente.dre." + System.currentTimeMillis() + "@isp.com.br")
                .phone("11966665555")
                .build());

        Plan plan = planRepository.save(Plan.builder()
                .name("Fibra 600 Mega Pro")
                .downloadSpeed(600)
                .uploadSpeed(300)
                .price(new BigDecimal("120.00"))
                .build());

        Contract contract = contractRepository.save(Contract.builder()
                .customerId(customer.getId())
                .planId(plan.getId())
                .contractNumber("CTR-DRE-" + System.currentTimeMillis())
                .monthlyFee(new BigDecimal("120.00"))
                .dueDay(10)
                .installationAddress("Av. Brasil, 1000")
                .city("Santarém")
                .state("PA")
                .zipCode("68000-000")
                .status(Contract.ContractStatus.ACTIVE)
                .build());

        // Fatura paga no mês
        invoiceRepository.save(Invoice.builder()
                .contractId(contract.getId())
                .customerId(customer.getId())
                .amount(new BigDecimal("120.00"))
                .dueDate(today)
                .status(Invoice.InvoiceStatus.PAID)
                .paidAt(LocalDateTime.now())
                .paidAmount(new BigDecimal("120.00"))
                .build());

        // 2. Criar Despesas pelo Plano de Contas Canônico seedado
        // Imposto (02.01.01)
        ChartOfAccount taxAcc = chartOfAccountRepository.findByCode("02.01.01").orElseThrow();
        payableInvoiceService.createPayableInvoice(PayableInvoiceRequest.builder()
                .supplierName("Receita Federal / Simples")
                .chartOfAccountId(taxAcc.getId())
                .description("DAS Telecom do mês")
                .totalAmount(new BigDecimal("10.00"))
                .installmentsCount(1)
                .firstDueDate(today)
                .build());

        // Interconexão IP (03.01.01)
        ChartOfAccount ipAcc = chartOfAccountRepository.findByCode("03.01.01").orElseThrow();
        payableInvoiceService.createPayableInvoice(PayableInvoiceRequest.builder()
                .supplierName("Tier 1 IP Transit")
                .chartOfAccountId(ipAcc.getId())
                .description("Trânsito IP 10Gbps")
                .totalAmount(new BigDecimal("20.00"))
                .installmentsCount(1)
                .firstDueDate(today)
                .build());

        // OPEX Postes (04.01.01)
        ChartOfAccount polesAcc = chartOfAccountRepository.findByCode("04.01.01").orElseThrow();
        payableInvoiceService.createPayableInvoice(PayableInvoiceRequest.builder()
                .supplierName("Concessionária de Energia")
                .chartOfAccountId(polesAcc.getId())
                .description("Compartilhamento de Postes")
                .totalAmount(new BigDecimal("30.00"))
                .installmentsCount(1)
                .firstDueDate(today)
                .build());

        // CAPEX Fibras parcelado em 12x (05.01.01)
        ChartOfAccount capexAcc = chartOfAccountRepository.findByCode("05.01.01").orElseThrow();
        payableInvoiceService.createPayableInvoice(PayableInvoiceRequest.builder()
                .supplierName("Fábrica de Cabos")
                .chartOfAccountId(capexAcc.getId())
                .description("Lote de Cabos Ópticos 12x")
                .totalAmount(new BigDecimal("1200.00"))
                .installmentsCount(12)
                .firstDueDate(today.plusMonths(1))
                .build());

        // 3. Executar e validar a DRE de Telecom em Tempo Real
        DreReportDto dre = dreReportService.generateDre(startOfMonth, endOfMonth, AccountingMethod.ACCRUAL);
        assertThat(dre).isNotNull();
        assertThat(dre.getGrossRevenue()).isGreaterThanOrEqualTo(new BigDecimal("120.00"));
        assertThat(dre.getTaxDeductions()).isGreaterThanOrEqualTo(new BigDecimal("10.00"));
        assertThat(dre.getDirectCostsInterconnection()).isGreaterThanOrEqualTo(new BigDecimal("20.00"));
        assertThat(dre.getTotalOpex()).isGreaterThanOrEqualTo(new BigDecimal("30.00"));
        assertThat(dre.getEbitda()).isNotNull();

        // 4. Executar e validar a Curva de Desalavancagem contínua de 36 meses
        DeleveragingProjectionDto projection = deleveragingEngineService.calculate36MonthsProjection();
        assertThat(projection).isNotNull();
        assertThat(projection.getTimeline()).hasSize(36);
        assertThat(projection.getCurrentMrr()).isGreaterThanOrEqualTo(new BigDecimal("120.00"));
        assertThat(projection.getWorstMonthYearMonth()).isNotNull();
        assertThat(projection.getBreakEvenYearMonth()).isNotNull();

        // 5. Testar o Simulador Interativo "E Se...?"
        SimulationRequest simRequest = SimulationRequest.builder()
                .description("Nova Máquina de Fusão Óptica")
                .totalAmount(new BigDecimal("12000.00"))
                .installmentsCount(12)
                .firstDueDate(today.plusMonths(1))
                .build();

        SimulationResponse simResult = deleveragingEngineService.simulateNewInvestment(simRequest);
        assertThat(simResult).isNotNull();
        assertThat(simResult.getMonthlyInstallmentAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(simResult.getRiskAnalysisSummary()).isNotBlank();
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
