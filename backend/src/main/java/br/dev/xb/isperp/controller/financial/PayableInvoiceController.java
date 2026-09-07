package br.dev.xb.isperp.controller.financial;

import br.dev.xb.isperp.api.contract.PayableInvoicesApi;
import br.dev.xb.isperp.api.dto.ExpenseInstallmentDto;
import br.dev.xb.isperp.api.dto.PayableInvoiceDto;
import br.dev.xb.isperp.api.dto.PayableInvoiceRequest;
import br.dev.xb.isperp.mapper.FinancialDomainMapper;
import br.dev.xb.isperp.service.financial.PayableInvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"", "/api"})
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PayableInvoiceController implements PayableInvoicesApi {

    private final PayableInvoiceService payableInvoiceService;
    private final FinancialDomainMapper financialDomainMapper;

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'CFO', 'DIRECTOR', 'FINANCIAL')")
    public ResponseEntity<List<PayableInvoiceDto>> getAllPayableInvoices() {
        return ResponseEntity.ok(financialDomainMapper.toOpenApiPayableInvoiceList(payableInvoiceService.getAllPayables()));
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'CFO', 'DIRECTOR', 'FINANCIAL')")
    public ResponseEntity<List<ExpenseInstallmentDto>> getPendingExpenseInstallments() {
        return ResponseEntity.ok(financialDomainMapper.toOpenApiExpenseInstallmentList(payableInvoiceService.getPendingInstallments()));
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'CFO', 'DIRECTOR', 'FINANCIAL')")
    public ResponseEntity<PayableInvoiceDto> createPayableInvoice(PayableInvoiceRequest request) {
        var domainRequest = financialDomainMapper.toDomainPayableInvoiceRequest(request);
        var created = payableInvoiceService.createPayableInvoice(domainRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(financialDomainMapper.toOpenApiPayableInvoice(created));
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'CFO', 'DIRECTOR', 'FINANCIAL')")
    public ResponseEntity<ExpenseInstallmentDto> payExpenseInstallment(UUID id, Double paidAmount, String paymentMethod, String receiptUrl) {
        BigDecimal amount = paidAmount != null ? BigDecimal.valueOf(paidAmount) : null;
        var paid = payableInvoiceService.payInstallment(id, amount, paymentMethod, receiptUrl);
        return ResponseEntity.ok(financialDomainMapper.toOpenApiExpenseInstallment(paid));
    }
}
