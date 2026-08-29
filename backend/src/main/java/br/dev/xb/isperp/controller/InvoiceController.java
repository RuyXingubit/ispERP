package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.entity.Contract;
import br.dev.xb.isperp.entity.Invoice;
import br.dev.xb.isperp.service.BillingScheduler;
import br.dev.xb.isperp.service.ContractService;
import br.dev.xb.isperp.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/invoices")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@SuppressWarnings("null")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final ContractService contractService;
    private final BillingScheduler billingScheduler;

    @GetMapping
    public ResponseEntity<List<Invoice>> getAllInvoices(
            @RequestParam(required = false) Invoice.InvoiceStatus status) {
        if (status != null) {
            return ResponseEntity.ok(invoiceService.getInvoicesByStatus(status));
        }
        return ResponseEntity.ok(invoiceService.getAllInvoices());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Invoice> getInvoiceById(@PathVariable @NonNull UUID id) {
        Optional<Invoice> invoice = invoiceService.getInvoiceById(id);
        return invoice.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Invoice>> getInvoicesByCustomerId(@PathVariable @NonNull UUID customerId) {
        return ResponseEntity.ok(invoiceService.getInvoicesByCustomerId(customerId));
    }

    @GetMapping("/contract/{contractId}")
    public ResponseEntity<List<Invoice>> getInvoicesByContractId(@PathVariable @NonNull UUID contractId) {
        return ResponseEntity.ok(invoiceService.getInvoicesByContractId(contractId));
    }

    @PostMapping("/generate/contract/{contractId}")
    public ResponseEntity<?> generateInvoiceManually(
            @PathVariable @NonNull UUID contractId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate) {
        try {
            Contract contract = contractService.getContractById(contractId)
                    .orElseThrow(() -> new RuntimeException("Contrato não encontrado"));

            LocalDate targetDueDate = (dueDate != null) ? dueDate : LocalDate.now().plusDays(10);
            Invoice created = invoiceService.createInvoiceForContract(contract, targetDueDate);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<?> markAsPaid(
            @PathVariable @NonNull UUID id,
            @RequestParam(required = false) BigDecimal paidAmount,
            @RequestParam(defaultValue = "PIX") String paymentMethod) {
        try {
            Invoice paid = invoiceService.markInvoiceAsPaid(id, paidAmount, paymentMethod);
            return ResponseEntity.ok(paid);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelInvoice(@PathVariable @NonNull UUID id) {
        try {
            Invoice canceled = invoiceService.cancelInvoice(id);
            return ResponseEntity.ok(canceled);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/trigger-recurring-billing")
    public ResponseEntity<String> triggerRecurringBilling() {
        billingScheduler.generateMonthlyInvoices();
        return ResponseEntity.ok("Rotina de faturamento recorrente executada com sucesso");
    }
}
