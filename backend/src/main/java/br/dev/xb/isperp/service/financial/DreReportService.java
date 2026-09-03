package br.dev.xb.isperp.service.financial;

import br.dev.xb.isperp.dto.financial.AccountingMethod;
import br.dev.xb.isperp.dto.financial.DreReportDto;
import br.dev.xb.isperp.entity.Invoice;
import br.dev.xb.isperp.entity.financial.ExpenseInstallment;
import br.dev.xb.isperp.entity.financial.PayableInvoice;
import br.dev.xb.isperp.entity.financial.PayableStatus;
import br.dev.xb.isperp.repository.InvoiceRepository;
import br.dev.xb.isperp.repository.financial.ExpenseInstallmentRepository;
import br.dev.xb.isperp.repository.financial.PayableInvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DreReportService {

    private final InvoiceRepository invoiceRepository;
    private final PayableInvoiceRepository payableInvoiceRepository;
    private final ExpenseInstallmentRepository installmentRepository;

    @Transactional(readOnly = true)
    public DreReportDto generateDre(LocalDate start, LocalDate end, AccountingMethod method) {
        log.info("Gerando DRE Telecom: de {} até {}, Regime={}", start, end, method);

        BigDecimal grossRevenue = calculateGrossRevenue(start, end, method);

        // Agrupamentos de despesas
        BigDecimal taxDeductions = BigDecimal.ZERO;
        BigDecimal directInterconnection = BigDecimal.ZERO;
        BigDecimal opexHr = BigDecimal.ZERO;
        BigDecimal opexPoles = BigDecimal.ZERO;
        BigDecimal opexFleet = BigDecimal.ZERO;
        BigDecimal opexMarketing = BigDecimal.ZERO;
        BigDecimal opexAdmin = BigDecimal.ZERO;
        BigDecimal capexAmortization = BigDecimal.ZERO;

        if (method == AccountingMethod.ACCRUAL) {
            // Regime de Competência: baseia-se na data de emissão das faturas a pagar
            List<PayableInvoice> payables = payableInvoiceRepository.findAll().stream()
                    .filter(p -> p.getIssueDate() != null &&
                            !p.getIssueDate().isBefore(start) &&
                            !p.getIssueDate().isAfter(end) &&
                            p.getStatus() != PayableStatus.CANCELLED)
                    .toList();

            for (PayableInvoice p : payables) {
                if (p.getChartOfAccount() == null || p.getChartOfAccount().getDreCategory() == null) continue;
                BigDecimal amount = p.getTotalAmount() != null ? p.getTotalAmount() : BigDecimal.ZERO;

                switch (p.getChartOfAccount().getDreCategory()) {
                    case TAX_DEDUCTION -> taxDeductions = taxDeductions.add(amount);
                    case DIRECT_COST_INTERCONNECTION -> directInterconnection = directInterconnection.add(amount);
                    case OPEX_HR -> opexHr = opexHr.add(amount);
                    case OPEX_POLES -> opexPoles = opexPoles.add(amount);
                    case OPEX_FLEET -> opexFleet = opexFleet.add(amount);
                    case OPEX_MARKETING -> opexMarketing = opexMarketing.add(amount);
                    case OPEX_ADMIN -> opexAdmin = opexAdmin.add(amount);
                    case CAPEX_NETWORK, CAPEX_EQUIPMENT, CAPEX_FLEET -> capexAmortization = capexAmortization.add(amount);
                }
            }
        } else {
            // Regime de Caixa: baseia-se na data de liquidação das parcelas
            OffsetDateTime startDt = start.atStartOfDay().atOffset(OffsetDateTime.now().getOffset());
            OffsetDateTime endDt = end.atTime(LocalTime.MAX).atOffset(OffsetDateTime.now().getOffset());

            List<ExpenseInstallment> paidInstallments = installmentRepository.findAll().stream()
                    .filter(inst -> inst.getStatus() == PayableStatus.PAID &&
                            inst.getPaidAt() != null &&
                            !inst.getPaidAt().isBefore(startDt) &&
                            !inst.getPaidAt().isAfter(endDt))
                    .toList();

            for (ExpenseInstallment inst : paidInstallments) {
                PayableInvoice p = inst.getPayableInvoice();
                if (p == null || p.getChartOfAccount() == null || p.getChartOfAccount().getDreCategory() == null) continue;
                BigDecimal amount = inst.getPaidAmount() != null ? inst.getPaidAmount() : inst.getAmount();

                switch (p.getChartOfAccount().getDreCategory()) {
                    case TAX_DEDUCTION -> taxDeductions = taxDeductions.add(amount);
                    case DIRECT_COST_INTERCONNECTION -> directInterconnection = directInterconnection.add(amount);
                    case OPEX_HR -> opexHr = opexHr.add(amount);
                    case OPEX_POLES -> opexPoles = opexPoles.add(amount);
                    case OPEX_FLEET -> opexFleet = opexFleet.add(amount);
                    case OPEX_MARKETING -> opexMarketing = opexMarketing.add(amount);
                    case OPEX_ADMIN -> opexAdmin = opexAdmin.add(amount);
                    case CAPEX_NETWORK, CAPEX_EQUIPMENT, CAPEX_FLEET -> capexAmortization = capexAmortization.add(amount);
                }
            }
        }

        // Fórmulas da Demonstração do Resultado
        BigDecimal netRevenue = grossRevenue.subtract(taxDeductions);
        BigDecimal contributionMargin = netRevenue.subtract(directInterconnection);
        BigDecimal totalOpex = opexHr.add(opexPoles).add(opexFleet).add(opexMarketing).add(opexAdmin);
        BigDecimal ebitda = contributionMargin.subtract(totalOpex);

        BigDecimal ebitdaMargin = BigDecimal.ZERO;
        if (grossRevenue.compareTo(BigDecimal.ZERO) > 0) {
            ebitdaMargin = ebitda.multiply(BigDecimal.valueOf(100))
                    .divide(grossRevenue, 2, RoundingMode.HALF_UP);
        }

        BigDecimal freeCashFlow = ebitda.subtract(capexAmortization);

        return DreReportDto.builder()
                .periodStart(start)
                .periodEnd(end)
                .accountingMethod(method)
                .grossRevenue(grossRevenue)
                .taxDeductions(taxDeductions)
                .netRevenue(netRevenue)
                .directCostsInterconnection(directInterconnection)
                .contributionMargin(contributionMargin)
                .opexHr(opexHr)
                .opexPoles(opexPoles)
                .opexFleet(opexFleet)
                .opexMarketing(opexMarketing)
                .opexAdmin(opexAdmin)
                .totalOpex(totalOpex)
                .ebitda(ebitda)
                .ebitdaMarginPercentage(ebitdaMargin)
                .capexAmortization(capexAmortization)
                .freeCashFlow(freeCashFlow)
                .build();
    }

    private BigDecimal calculateGrossRevenue(LocalDate start, LocalDate end, AccountingMethod method) {
        List<Invoice> invoices = invoiceRepository.findAll();
        BigDecimal total = BigDecimal.ZERO;

        for (Invoice inv : invoices) {
            if (inv.getStatus() == Invoice.InvoiceStatus.CANCELED) continue;

            if (method == AccountingMethod.ACCRUAL) {
                // Competência: considera a data de vencimento da fatura
                if (inv.getDueDate() != null && !inv.getDueDate().isBefore(start) && !inv.getDueDate().isAfter(end)) {
                    total = total.add(inv.getAmount() != null ? inv.getAmount() : BigDecimal.ZERO);
                }
            } else {
                // Caixa: considera quando o cliente efetivamente pagou
                if (inv.getStatus() == Invoice.InvoiceStatus.PAID && inv.getPaidAt() != null) {
                    LocalDate paidDate = inv.getPaidAt().toLocalDate();
                    if (!paidDate.isBefore(start) && !paidDate.isAfter(end)) {
                        total = total.add(inv.getPaidAmount() != null ? inv.getPaidAmount() : inv.getAmount());
                    }
                }
            }
        }
        return total;
    }
}
