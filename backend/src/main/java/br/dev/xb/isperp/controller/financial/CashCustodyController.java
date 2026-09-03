package br.dev.xb.isperp.controller.financial;

import br.dev.xb.isperp.dto.financial.*;
import br.dev.xb.isperp.service.financial.CashCustodyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/financial/custody/cash")
@RequiredArgsConstructor
@Tag(name = "Custódia de Dinheiro Vivo", description = "Endpoints para rastreabilidade de dinheiro vivo por CPF, duplo aceite e conciliação bancária")
public class CashCustodyController {

    private final CashCustodyService cashCustodyService;

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'CFO', 'DIRECTOR', 'FINANCIAL')")
    @Operation(summary = "Lista todos os saldos sob custódia de colaboradores na empresa")
    public ResponseEntity<List<CashCustodyDto>> getAllCustodies() {
        return ResponseEntity.ok(cashCustodyService.getAllCustodies());
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CFO', 'DIRECTOR', 'FINANCIAL')")
    @Operation(summary = "Consulta o saldo sob custódia de um colaborador específico")
    public ResponseEntity<CashCustodyDto> getCustodyByUserId(@PathVariable UUID userId) {
        return ResponseEntity.ok(cashCustodyService.getCustodyDtoByUserId(userId));
    }

    @PostMapping("/settle")
    @Operation(summary = "Registra recebimento de fatura em dinheiro vivo e debita na custódia do CPF do colaborador")
    public ResponseEntity<CashCustodyDto> recordCashSettlement(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody CashSettlementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cashCustodyService.recordCashSettlement(userId, request));
    }

    @PostMapping("/transfer/request")
    @Operation(summary = "Solicita transferência de valores para outro colaborador (Passagem de Gaveta/Turno)")
    public ResponseEntity<CashTransferResponseDto> requestTransfer(
            @RequestHeader("X-User-Id") UUID senderUserId,
            @Valid @RequestBody CashTransferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cashCustodyService.requestTransfer(senderUserId, request));
    }

    @PostMapping("/transfer/{id}/respond")
    @Operation(summary = "Aceita ou rejeita transferência de custódia recebida (Duplo Aceite)")
    public ResponseEntity<CashTransferResponseDto> respondTransfer(
            @RequestHeader("X-User-Id") UUID receiverUserId,
            @PathVariable UUID id,
            @RequestParam boolean accept) {
        return ResponseEntity.ok(cashCustodyService.respondTransfer(receiverUserId, id, accept));
    }

    @GetMapping("/transfer/pending")
    @Operation(summary = "Lista transferências de dinheiro pendentes de aceite para o colaborador logado")
    public ResponseEntity<List<CashTransferResponseDto>> getPendingTransfers(
            @RequestHeader("X-User-Id") UUID receiverUserId) {
        return ResponseEntity.ok(cashCustodyService.getPendingTransfersForReceiver(receiverUserId));
    }

    @PostMapping("/deposit")
    @Operation(summary = "Submete comprovante de depósito bancário para prestação de contas de dinheiro vivo")
    public ResponseEntity<BankDepositResponseDto> submitBankDeposit(
            @RequestHeader("X-User-Id") UUID depositorUserId,
            @Valid @RequestBody BankDepositRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cashCustodyService.submitBankDeposit(depositorUserId, request));
    }

    @GetMapping("/deposit/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'CFO', 'DIRECTOR', 'FINANCIAL')")
    @Operation(summary = "Lista depósitos bancários pendentes de conciliação pelo CFO/Auditor")
    public ResponseEntity<List<BankDepositResponseDto>> getPendingDeposits() {
        return ResponseEntity.ok(cashCustodyService.getPendingBankDeposits());
    }

    @PostMapping("/deposit/{id}/audit")
    @PreAuthorize("hasAnyRole('ADMIN', 'CFO', 'DIRECTOR', 'FINANCIAL')")
    @Operation(summary = "Audita e concilia depósito bancário pelo extrato real, liquidando a custódia do colaborador")
    public ResponseEntity<BankDepositResponseDto> auditBankDeposit(
            @RequestHeader("X-User-Id") UUID auditorUserId,
            @PathVariable UUID id,
            @Valid @RequestBody BankDepositAuditRequest request) {
        return ResponseEntity.ok(cashCustodyService.auditBankDeposit(auditorUserId, id, request));
    }
}
