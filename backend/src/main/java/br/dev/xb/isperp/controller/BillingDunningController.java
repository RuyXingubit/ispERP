package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.contract.BillingDunningApi;
import br.dev.xb.isperp.api.dto.CrossCreditRebalanceResponse;
import br.dev.xb.isperp.api.dto.DunningProcessResponse;
import br.dev.xb.isperp.api.dto.UnblockEvaluationResultResponse;
import br.dev.xb.isperp.service.HierarchicalBillingService;
import br.dev.xb.isperp.service.InvoiceRebalanceService;
import br.dev.xb.isperp.service.TrustUnblockPolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@SuppressWarnings("null")
public class BillingDunningController implements BillingDunningApi {

    private final HierarchicalBillingService hierarchicalBillingService;
    private final InvoiceRebalanceService invoiceRebalanceService;
    private final TrustUnblockPolicyService trustUnblockPolicyService;

    @Override
    public ResponseEntity<DunningProcessResponse> processDailyDunning() {
        int suspended = hierarchicalBillingService.processDailyDunning(LocalDateTime.now());
        DunningProcessResponse response = new DunningProcessResponse();
        response.setSuccess(true);
        response.setSuspendedCount(suspended);
        response.setProcessedAt(LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<CrossCreditRebalanceResponse> executeCrossCreditRebalance(
            UUID futurePaidInvoiceId,
            UUID overdueUnpaidInvoiceId) {
        invoiceRebalanceService.executeCrossCreditRebalance(futurePaidInvoiceId, overdueUnpaidInvoiceId);
        CrossCreditRebalanceResponse response = new CrossCreditRebalanceResponse();
        response.setSuccess(true);
        response.setMessage("Compensação cruzada realizada com sucesso e avisos fixos registrados.");
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<UnblockEvaluationResultResponse> requestBotTrustUnblock(UUID contractId) {
        TrustUnblockPolicyService.UnblockEvaluationResult result = trustUnblockPolicyService.requestBotAutoUnblock(contractId);
        return ResponseEntity.ok(toResponse(result));
    }

    @Override
    public ResponseEntity<UnblockEvaluationResultResponse> requestAttendantTrustUnblock(
            UUID contractId,
            UUID attendantUserId,
            String reason) {
        TrustUnblockPolicyService.UnblockEvaluationResult result = trustUnblockPolicyService.requestAttendantManualUnblock(contractId, attendantUserId, reason);
        return ResponseEntity.ok(toResponse(result));
    }

    private UnblockEvaluationResultResponse toResponse(TrustUnblockPolicyService.UnblockEvaluationResult result) {
        UnblockEvaluationResultResponse response = new UnblockEvaluationResultResponse();
        response.setGranted(result.isGranted());
        response.setMessage(result.getMessage());
        response.setUnblockType(result.getUnblockType());
        if (result.getExpiresAt() != null) {
            response.setExpiresAt(result.getExpiresAt().atOffset(ZoneOffset.UTC));
        }
        return response;
    }
}
