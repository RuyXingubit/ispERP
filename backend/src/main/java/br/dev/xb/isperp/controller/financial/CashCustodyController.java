package br.dev.xb.isperp.controller.financial;

import br.dev.xb.isperp.api.contract.CashCustodyApi;
import br.dev.xb.isperp.api.dto.*;
import br.dev.xb.isperp.mapper.FinancialDomainMapper;
import br.dev.xb.isperp.service.financial.CashCustodyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"", "/api"})
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CashCustodyController implements CashCustodyApi {

    private final CashCustodyService cashCustodyService;
    private final FinancialDomainMapper financialDomainMapper;

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'CFO', 'DIRECTOR', 'FINANCIAL')")
    public ResponseEntity<List<CashCustodyDto>> getAllCashCustodies() {
        return ResponseEntity.ok(financialDomainMapper.toOpenApiCashCustodyList(cashCustodyService.getAllCustodies()));
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'CFO', 'DIRECTOR', 'FINANCIAL')")
    public ResponseEntity<CashCustodyDto> getCashCustodyByUserId(UUID userId) {
        return ResponseEntity.ok(financialDomainMapper.toOpenApiCashCustody(cashCustodyService.getCustodyDtoByUserId(userId)));
    }

    @Override
    public ResponseEntity<CashCustodyDto> recordCashSettlement(UUID xUserId, CashSettlementRequest request) {
        var domainRequest = financialDomainMapper.toDomainCashSettlementRequest(request);
        var domainResult = cashCustodyService.recordCashSettlement(xUserId, domainRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(financialDomainMapper.toOpenApiCashCustody(domainResult));
    }

    @Override
    public ResponseEntity<CashTransferResponseDto> requestCashTransfer(UUID xUserId, CashTransferRequest request) {
        var domainRequest = financialDomainMapper.toDomainCashTransferRequest(request);
        var domainResult = cashCustodyService.requestTransfer(xUserId, domainRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(financialDomainMapper.toOpenApiCashTransferResponse(domainResult));
    }

    @Override
    public ResponseEntity<CashTransferResponseDto> respondCashTransfer(UUID xUserId, UUID id, Boolean accept) {
        var domainResult = cashCustodyService.respondTransfer(xUserId, id, Boolean.TRUE.equals(accept));
        return ResponseEntity.ok(financialDomainMapper.toOpenApiCashTransferResponse(domainResult));
    }

    @Override
    public ResponseEntity<List<CashTransferResponseDto>> getPendingCashTransfers(UUID xUserId) {
        return ResponseEntity.ok(financialDomainMapper.toOpenApiCashTransferResponseList(cashCustodyService.getPendingTransfersForReceiver(xUserId)));
    }

    @Override
    public ResponseEntity<BankDepositResponseDto> submitBankDeposit(UUID xUserId, BankDepositRequest request) {
        var domainRequest = financialDomainMapper.toDomainBankDepositRequest(request);
        var domainResult = cashCustodyService.submitBankDeposit(xUserId, domainRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(financialDomainMapper.toOpenApiBankDepositResponse(domainResult));
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'CFO', 'DIRECTOR', 'FINANCIAL')")
    public ResponseEntity<List<BankDepositResponseDto>> getPendingBankDeposits() {
        return ResponseEntity.ok(financialDomainMapper.toOpenApiBankDepositResponseList(cashCustodyService.getPendingBankDeposits()));
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'CFO', 'DIRECTOR', 'FINANCIAL')")
    public ResponseEntity<BankDepositResponseDto> auditBankDeposit(UUID xUserId, UUID id, BankDepositAuditRequest request) {
        var domainRequest = financialDomainMapper.toDomainBankDepositAuditRequest(request);
        var domainResult = cashCustodyService.auditBankDeposit(xUserId, id, domainRequest);
        return ResponseEntity.ok(financialDomainMapper.toOpenApiBankDepositResponse(domainResult));
    }
}
