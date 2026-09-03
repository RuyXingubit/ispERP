package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.service.HierarchicalBillingService;
import br.dev.xb.isperp.service.InvoiceRebalanceService;
import br.dev.xb.isperp.service.TrustUnblockPolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping({"/billing/dunning", "/api/billing/dunning"})
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@SuppressWarnings("null")
public class BillingDunningController {

    private final HierarchicalBillingService hierarchicalBillingService;
    private final InvoiceRebalanceService invoiceRebalanceService;
    private final TrustUnblockPolicyService trustUnblockPolicyService;

    @PostMapping("/process")
    public ResponseEntity<Map<String, Object>> processDailyDunning() {
        int suspended = hierarchicalBillingService.processDailyDunning(LocalDateTime.now());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "suspendedCount", suspended,
                "processedAt", LocalDateTime.now().toString()
        ));
    }

    @PostMapping("/rebalance/cross-credit")
    public ResponseEntity<Map<String, Object>> executeCrossCredit(
            @RequestParam UUID futurePaidInvoiceId,
            @RequestParam UUID overdueUnpaidInvoiceId) {
        invoiceRebalanceService.executeCrossCreditRebalance(futurePaidInvoiceId, overdueUnpaidInvoiceId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Compensação cruzada realizada com sucesso e avisos fixos registrados."
        ));
    }

    @PostMapping("/trust-unblock/bot")
    public ResponseEntity<TrustUnblockPolicyService.UnblockEvaluationResult> requestBotUnblock(@RequestParam UUID contractId) {
        return ResponseEntity.ok(trustUnblockPolicyService.requestBotAutoUnblock(contractId));
    }

    @PostMapping("/trust-unblock/attendant")
    public ResponseEntity<TrustUnblockPolicyService.UnblockEvaluationResult> requestAttendantUnblock(
            @RequestParam UUID contractId,
            @RequestParam(required = false) UUID attendantUserId,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(trustUnblockPolicyService.requestAttendantManualUnblock(contractId, attendantUserId, reason));
    }
}
