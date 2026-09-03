package br.dev.xb.isperp.controller.financial;

import br.dev.xb.isperp.dto.financial.ExpenseInstallmentDto;
import br.dev.xb.isperp.dto.financial.PayableInvoiceDto;
import br.dev.xb.isperp.dto.financial.PayableInvoiceRequest;
import br.dev.xb.isperp.service.financial.PayableInvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/financial/payables")
@RequiredArgsConstructor
@Tag(name = "Contas a Pagar & Parcelamentos", description = "Endpoints para gestão de compras, CAPEX e parcelamento de dívidas com fornecedores")
public class PayableInvoiceController {

    private final PayableInvoiceService payableInvoiceService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CFO', 'DIRECTOR', 'FINANCIAL')")
    @Operation(summary = "Lista todas as contas a pagar da empresa")
    public ResponseEntity<List<PayableInvoiceDto>> getAllPayables() {
        return ResponseEntity.ok(payableInvoiceService.getAllPayables());
    }

    @GetMapping("/installments/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'CFO', 'DIRECTOR', 'FINANCIAL')")
    @Operation(summary = "Lista todas as parcelas de despesas e compras pendentes de pagamento")
    public ResponseEntity<List<ExpenseInstallmentDto>> getPendingInstallments() {
        return ResponseEntity.ok(payableInvoiceService.getPendingInstallments());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CFO', 'DIRECTOR', 'FINANCIAL')")
    @Operation(summary = "Cadastra uma nova conta a pagar com geração automática de parcelas de longo prazo")
    public ResponseEntity<PayableInvoiceDto> createPayable(@Valid @RequestBody PayableInvoiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(payableInvoiceService.createPayableInvoice(request));
    }

    @PostMapping("/installments/{id}/pay")
    @PreAuthorize("hasAnyRole('ADMIN', 'CFO', 'DIRECTOR', 'FINANCIAL')")
    @Operation(summary = "Baixa o pagamento de uma parcela de despesa")
    public ResponseEntity<ExpenseInstallmentDto> payInstallment(
            @PathVariable UUID id,
            @RequestParam(required = false) BigDecimal paidAmount,
            @RequestParam(required = false, defaultValue = "TRANSFERENCIA_BANCARIA") String paymentMethod,
            @RequestParam(required = false) String receiptUrl) {
        return ResponseEntity.ok(payableInvoiceService.payInstallment(id, paidAmount, paymentMethod, receiptUrl));
    }
}
