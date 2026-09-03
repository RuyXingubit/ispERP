package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.contract.InvoicesApi;
import br.dev.xb.isperp.api.dto.InvoiceResponse;
import br.dev.xb.isperp.api.dto.InvoiceStatus;
import br.dev.xb.isperp.api.dto.PayInvoiceRequest;
import br.dev.xb.isperp.api.dto.TriggerRecurringBillingResponse;
import br.dev.xb.isperp.entity.Contract;
import br.dev.xb.isperp.entity.Invoice;
import br.dev.xb.isperp.mapper.InvoiceMapper;
import br.dev.xb.isperp.service.BillingScheduler;
import br.dev.xb.isperp.service.ContractService;
import br.dev.xb.isperp.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@SuppressWarnings("null")
public class InvoiceController implements InvoicesApi {

    private final InvoiceService invoiceService;
    private final ContractService contractService;
    private final BillingScheduler billingScheduler;
    private final InvoiceMapper invoiceMapper;

    @Override
    public ResponseEntity<List<InvoiceResponse>> getAllInvoices(InvoiceStatus status) {
        if (status != null) {
            Invoice.InvoiceStatus domainStatus = invoiceMapper.toEntityStatus(status);
            return ResponseEntity.ok(invoiceMapper.toResponseList(invoiceService.getInvoicesByStatus(domainStatus)));
        }
        return ResponseEntity.ok(invoiceMapper.toResponseList(invoiceService.getAllInvoices()));
    }

    @Override
    public ResponseEntity<InvoiceResponse> getInvoiceById(UUID id) {
        Optional<Invoice> invoice = invoiceService.getInvoiceById(id);
        return invoice.map(invoiceMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<List<InvoiceResponse>> getInvoicesByCustomerId(UUID customerId) {
        return ResponseEntity.ok(invoiceMapper.toResponseList(invoiceService.getInvoicesByCustomerId(customerId)));
    }

    @Override
    public ResponseEntity<List<InvoiceResponse>> getInvoicesByContractId(UUID contractId) {
        return ResponseEntity.ok(invoiceMapper.toResponseList(invoiceService.getInvoicesByContractId(contractId)));
    }

    @Override
    public ResponseEntity<InvoiceResponse> generateInvoiceManually(UUID contractId, LocalDate dueDate) {
        Contract contract = contractService.getContractById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contrato não encontrado: " + contractId));

        LocalDate targetDueDate = (dueDate != null) ? dueDate : LocalDate.now().plusDays(10);
        Invoice created = invoiceService.createInvoiceForContract(contract, targetDueDate);
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceMapper.toResponse(created));
    }

    @Override
    public ResponseEntity<InvoiceResponse> payInvoice(UUID id, PayInvoiceRequest payInvoiceRequest) {
        BigDecimal paidAmount = null;
        String paymentMethod = "PIX";

        if (payInvoiceRequest != null) {
            if (payInvoiceRequest.getPaidAmount() != null) {
                paidAmount = BigDecimal.valueOf(payInvoiceRequest.getPaidAmount());
            }
            if (payInvoiceRequest.getPaymentMethod() != null && !payInvoiceRequest.getPaymentMethod().isBlank()) {
                paymentMethod = payInvoiceRequest.getPaymentMethod();
            }
        }

        Invoice paid = invoiceService.markInvoiceAsPaid(id, paidAmount, paymentMethod);
        return ResponseEntity.ok(invoiceMapper.toResponse(paid));
    }

    @Override
    public ResponseEntity<InvoiceResponse> cancelInvoice(UUID id) {
        Invoice canceled = invoiceService.cancelInvoice(id);
        return ResponseEntity.ok(invoiceMapper.toResponse(canceled));
    }

    @Override
    public ResponseEntity<TriggerRecurringBillingResponse> triggerRecurringBilling() {
        billingScheduler.generateMonthlyInvoices();
        return ResponseEntity.ok(new TriggerRecurringBillingResponse("Rotina de faturamento recorrente executada com sucesso"));
    }
}
