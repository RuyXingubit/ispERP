package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.contract.BillingDunningApi;
import br.dev.xb.isperp.api.dto.BillingNotificationDispatchResponse;
import br.dev.xb.isperp.api.dto.BillingNotificationSweepResponse;
import br.dev.xb.isperp.api.dto.CrossCreditRebalanceResponse;
import br.dev.xb.isperp.api.dto.DunningProcessResponse;
import br.dev.xb.isperp.api.dto.NotificationDunningStage;
import br.dev.xb.isperp.api.dto.UnblockEvaluationResultResponse;
import br.dev.xb.isperp.entity.BillingNotificationDispatch;
import br.dev.xb.isperp.service.BillingNotificationDunningService;
import br.dev.xb.isperp.service.HierarchicalBillingService;
import br.dev.xb.isperp.service.InvoiceRebalanceService;
import br.dev.xb.isperp.service.TrustUnblockPolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class BillingDunningController implements BillingDunningApi {

    private final HierarchicalBillingService hierarchicalBillingService;
    private final InvoiceRebalanceService invoiceRebalanceService;
    private final TrustUnblockPolicyService trustUnblockPolicyService;

    @Nullable
    private BillingNotificationDunningService billingNotificationDunningService;

    @Autowired(required = false)
    public void setBillingNotificationDunningService(@Nullable BillingNotificationDunningService billingNotificationDunningService) {
        this.billingNotificationDunningService = billingNotificationDunningService;
    }

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

    @Override
    public ResponseEntity<BillingNotificationSweepResponse> runOmnichannelDunningNotifications() {
        if (billingNotificationDunningService == null) {
            return ResponseEntity.badRequest().build();
        }
        BillingNotificationDunningService.SweepResult sweep =
                billingNotificationDunningService.runOmnichannelDunningNotifications(LocalDate.now());
        BillingNotificationSweepResponse resp = new BillingNotificationSweepResponse();
        resp.setTotalDispatched(sweep.totalDispatched());
        resp.setdMinus3Count(sweep.dMinus3Count());
        resp.setdZeroCount(sweep.dZeroCount());
        resp.setdPlus5AnatelCount(sweep.dPlus5AnatelCount());
        resp.setProcessedAt(sweep.processedAt());
        resp.setDispatches(sweep.dispatches().stream().map(this::toDispatchDto).toList());
        return ResponseEntity.ok(resp);
    }

    @Override
    public ResponseEntity<List<BillingNotificationDispatchResponse>> listOmnichannelDunningNotifications() {
        if (billingNotificationDunningService == null) {
            return ResponseEntity.ok(List.of());
        }
        List<BillingNotificationDispatchResponse> list = billingNotificationDunningService.listDispatches()
                .stream()
                .map(this::toDispatchDto)
                .toList();
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<BillingNotificationDispatchResponse> sendInvoiceDunningNotification(
            UUID invoiceId,
            @Nullable String channel) {
        if (billingNotificationDunningService == null) {
            return ResponseEntity.badRequest().build();
        }
        BillingNotificationDispatch dispatch =
                billingNotificationDunningService.sendManualInvoiceNotification(invoiceId, channel);
        return ResponseEntity.ok(toDispatchDto(dispatch));
    }

    private BillingNotificationDispatchResponse toDispatchDto(BillingNotificationDispatch entity) {
        BillingNotificationDispatchResponse dto = new BillingNotificationDispatchResponse();
        dto.setId(entity.getId());
        dto.setInvoiceId(entity.getInvoiceId());
        dto.setContractId(entity.getContractId());
        dto.setCustomerId(entity.getCustomerId());
        dto.setCustomerName(entity.getCustomerName());
        if (entity.getStage() != null) {
            dto.setStage(NotificationDunningStage.fromValue(entity.getStage().name()));
        }
        dto.setChannel(entity.getChannel());
        dto.setRecipient(entity.getRecipient());
        dto.setMessagePreview(entity.getMessagePreview());
        dto.setDeliveryStatus(entity.getDeliveryStatus());
        dto.setDispatchedAt(entity.getDispatchedAt());
        return dto;
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

