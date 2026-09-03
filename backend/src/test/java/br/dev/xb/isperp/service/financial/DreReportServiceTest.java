package br.dev.xb.isperp.service.financial;

import br.dev.xb.isperp.dto.financial.AccountingMethod;
import br.dev.xb.isperp.dto.financial.DreReportDto;
import br.dev.xb.isperp.entity.Invoice;
import br.dev.xb.isperp.entity.financial.ChartOfAccount;
import br.dev.xb.isperp.entity.financial.DreCategory;
import br.dev.xb.isperp.entity.financial.PayableInvoice;
import br.dev.xb.isperp.entity.financial.PayableStatus;
import br.dev.xb.isperp.repository.InvoiceRepository;
import br.dev.xb.isperp.repository.financial.ExpenseInstallmentRepository;
import br.dev.xb.isperp.repository.financial.PayableInvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DreReportServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private PayableInvoiceRepository payableInvoiceRepository;

    @Mock
    private ExpenseInstallmentRepository installmentRepository;

    private DreReportService dreReportService;

    @BeforeEach
    void setUp() {
        dreReportService = new DreReportService(invoiceRepository, payableInvoiceRepository, installmentRepository);
    }

    @Test
    @DisplayName("Deve calcular o EBITDA e Margem de Contribuição com precisão no Regime de Competência")
    void shouldCalculateEbitdaAndDreAccrual() {
        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 10, 31);

        // Receita Bruta: R$ 100.000,00
        Invoice inv1 = Invoice.builder()
                .id(UUID.randomUUID())
                .amount(new BigDecimal("100000.00"))
                .dueDate(LocalDate.of(2026, 10, 10))
                .status(Invoice.InvoiceStatus.PENDING)
                .build();
        when(invoiceRepository.findAll()).thenReturn(List.of(inv1));

        // Deduções / Impostos: R$ 6.000,00 (DAS Telecom)
        ChartOfAccount taxAccount = ChartOfAccount.builder().dreCategory(DreCategory.TAX_DEDUCTION).build();
        PayableInvoice taxExpense = PayableInvoice.builder()
                .chartOfAccount(taxAccount)
                .totalAmount(new BigDecimal("6000.00"))
                .issueDate(LocalDate.of(2026, 10, 5))
                .status(PayableStatus.PENDING)
                .build();

        // Interconexão / Trânsito IP: R$ 10.000,00
        ChartOfAccount linkAccount = ChartOfAccount.builder().dreCategory(DreCategory.DIRECT_COST_INTERCONNECTION).build();
        PayableInvoice linkExpense = PayableInvoice.builder()
                .chartOfAccount(linkAccount)
                .totalAmount(new BigDecimal("10000.00"))
                .issueDate(LocalDate.of(2026, 10, 8))
                .status(PayableStatus.PENDING)
                .build();

        // OPEX Postes e Folha: R$ 30.000,00
        ChartOfAccount opexAccount = ChartOfAccount.builder().dreCategory(DreCategory.OPEX_POLES).build();
        PayableInvoice opexExpense = PayableInvoice.builder()
                .chartOfAccount(opexAccount)
                .totalAmount(new BigDecimal("30000.00"))
                .issueDate(LocalDate.of(2026, 10, 15))
                .status(PayableStatus.PENDING)
                .build();

        // CAPEX Fibras: R$ 20.000,00
        ChartOfAccount capexAccount = ChartOfAccount.builder().dreCategory(DreCategory.CAPEX_NETWORK).build();
        PayableInvoice capexExpense = PayableInvoice.builder()
                .chartOfAccount(capexAccount)
                .totalAmount(new BigDecimal("20000.00"))
                .issueDate(LocalDate.of(2026, 10, 20))
                .status(PayableStatus.PENDING)
                .build();

        when(payableInvoiceRepository.findAll()).thenReturn(List.of(taxExpense, linkExpense, opexExpense, capexExpense));

        DreReportDto dre = dreReportService.generateDre(start, end, AccountingMethod.ACCRUAL);

        // Asserções das Fórmulas de Telecom:
        // Receita Bruta = 100.000
        assertThat(dre.getGrossRevenue()).isEqualByComparingTo(new BigDecimal("100000.00"));
        // Receita Líquida = 100.000 - 6.000 = 94.000
        assertThat(dre.getNetRevenue()).isEqualByComparingTo(new BigDecimal("94000.00"));
        // Margem de Contribuição = 94.000 - 10.000 = 84.000
        assertThat(dre.getContributionMargin()).isEqualByComparingTo(new BigDecimal("84000.00"));
        // OPEX = 30.000
        assertThat(dre.getTotalOpex()).isEqualByComparingTo(new BigDecimal("30000.00"));
        // EBITDA = 84.000 - 30.000 = 54.000 (54% de margem EBITDA)
        assertThat(dre.getEbitda()).isEqualByComparingTo(new BigDecimal("54000.00"));
        assertThat(dre.getEbitdaMarginPercentage()).isEqualByComparingTo(new BigDecimal("54.00"));
        // Fluxo de Caixa Livre = 54.000 - 20.000 (CAPEX) = 34.000
        assertThat(dre.getFreeCashFlow()).isEqualByComparingTo(new BigDecimal("34000.00"));
    }
}
