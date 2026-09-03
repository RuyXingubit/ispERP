package br.dev.xb.isperp.service.financial;

import br.dev.xb.isperp.dto.financial.ExpenseInstallmentDto;
import br.dev.xb.isperp.dto.financial.PayableInvoiceDto;
import br.dev.xb.isperp.dto.financial.PayableInvoiceRequest;
import br.dev.xb.isperp.entity.financial.ChartOfAccount;
import br.dev.xb.isperp.entity.financial.ExpenseInstallment;
import br.dev.xb.isperp.entity.financial.PayableInvoice;
import br.dev.xb.isperp.entity.financial.PayableStatus;
import br.dev.xb.isperp.exception.ResourceNotFoundException;
import br.dev.xb.isperp.mapper.FinancialAccountMapper;
import br.dev.xb.isperp.repository.financial.ChartOfAccountRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayableInvoiceService {

    private final PayableInvoiceRepository payableInvoiceRepository;
    private final ExpenseInstallmentRepository installmentRepository;
    private final ChartOfAccountRepository chartOfAccountRepository;
    private final FinancialAccountMapper financialAccountMapper;

    @Transactional(readOnly = true)
    public List<PayableInvoiceDto> getAllPayables() {
        return financialAccountMapper.toPayableInvoiceDtoList(payableInvoiceRepository.findAll());
    }

    @Transactional(readOnly = true)
    public List<ExpenseInstallmentDto> getPendingInstallments() {
        return financialAccountMapper.toExpenseInstallmentDtoList(
                installmentRepository.findByStatusOrderByDueDateAsc(PayableStatus.PENDING));
    }

    @Transactional(readOnly = true)
    public List<ExpenseInstallmentDto> getInstallmentsByPeriod(LocalDate start, LocalDate end) {
        return financialAccountMapper.toExpenseInstallmentDtoList(
                installmentRepository.findByDueDateBetweenOrderByDueDateAsc(start, end));
    }

    @Transactional
    public PayableInvoiceDto createPayableInvoice(PayableInvoiceRequest request) {
        ChartOfAccount account = chartOfAccountRepository.findById(request.getChartOfAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Conta contábil não encontrada: " + request.getChartOfAccountId()));

        if (Boolean.TRUE.equals(account.getIsSynthetic())) {
            throw new IllegalArgumentException("Não é permitido lançar despesas em contas sintéticas (apenas analíticas).");
        }

        PayableInvoice invoice = PayableInvoice.builder()
                .supplierName(request.getSupplierName())
                .supplierDocument(request.getSupplierDocument())
                .chartOfAccount(account)
                .description(request.getDescription())
                .invoiceNumber(request.getInvoiceNumber())
                .totalAmount(request.getTotalAmount())
                .issueDate(request.getIssueDate() != null ? request.getIssueDate() : LocalDate.now())
                .status(PayableStatus.PENDING)
                .notes(request.getNotes())
                .build();

        PayableInvoice savedInvoice = payableInvoiceRepository.save(invoice);

        // Geração automática das parcelas (esteira de amortização de CAPEX / Dívidas)
        int totalInstallments = request.getInstallmentsCount() != null ? request.getInstallmentsCount() : 1;
        BigDecimal installmentAmount = request.getTotalAmount().divide(BigDecimal.valueOf(totalInstallments), 2, RoundingMode.HALF_UP);
        LocalDate firstDue = request.getFirstDueDate() != null ? request.getFirstDueDate() : LocalDate.now().plusMonths(1);

        List<ExpenseInstallment> installments = new ArrayList<>();
        BigDecimal accumulated = BigDecimal.ZERO;

        for (int i = 1; i <= totalInstallments; i++) {
            BigDecimal currentAmount = (i == totalInstallments)
                    ? request.getTotalAmount().subtract(accumulated)
                    : installmentAmount;
            accumulated = accumulated.add(currentAmount);

            ExpenseInstallment installment = ExpenseInstallment.builder()
                    .payableInvoice(savedInvoice)
                    .installmentNumber(i)
                    .totalInstallments(totalInstallments)
                    .dueDate(firstDue.plusMonths(i - 1L))
                    .amount(currentAmount)
                    .status(PayableStatus.PENDING)
                    .build();

            installments.add(installment);
        }

        installmentRepository.saveAll(installments);
        savedInvoice.setInstallments(installments);

        log.info("Conta a pagar criada: Fornecedor={}, Valor=R${}, Parcelas={}",
                savedInvoice.getSupplierName(), savedInvoice.getTotalAmount(), totalInstallments);

        return financialAccountMapper.toDto(savedInvoice);
    }

    @Transactional
    public ExpenseInstallmentDto payInstallment(UUID installmentId, BigDecimal paidAmount, String paymentMethod, String receiptUrl) {
        ExpenseInstallment installment = installmentRepository.findById(installmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Parcela de despesa não encontrada: " + installmentId));

        if (installment.getStatus() == PayableStatus.PAID) {
            throw new IllegalStateException("Esta parcela já foi quitada anteriormente.");
        }

        installment.setStatus(PayableStatus.PAID);
        installment.setPaidAt(OffsetDateTime.now());
        installment.setPaidAmount(paidAmount != null ? paidAmount : installment.getAmount());
        installment.setPaymentMethod(paymentMethod != null ? paymentMethod : "TRANSFERENCIA_BANCARIA");
        installment.setReceiptUrl(receiptUrl);

        ExpenseInstallment savedInstallment = installmentRepository.save(installment);

        // Atualizar status da fatura mãe
        PayableInvoice parent = savedInstallment.getPayableInvoice();
        List<ExpenseInstallment> all = installmentRepository.findByPayableInvoiceIdOrderByInstallmentNumberAsc(parent.getId());
        boolean allPaid = all.stream().allMatch(inst -> inst.getStatus() == PayableStatus.PAID);

        parent.setStatus(allPaid ? PayableStatus.PAID : PayableStatus.PARTIALLY_PAID);
        payableInvoiceRepository.save(parent);

        log.info("Parcela {}/{} quitada para {}. Valor: R$ {}",
                savedInstallment.getInstallmentNumber(), savedInstallment.getTotalInstallments(),
                parent.getSupplierName(), savedInstallment.getPaidAmount());

        return financialAccountMapper.toDto(savedInstallment);
    }
}
