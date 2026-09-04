package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.contract.FiscalRegimeTransitionsApi;
import br.dev.xb.isperp.api.dto.FiscalRegimeTransitionRequest;
import br.dev.xb.isperp.api.dto.FiscalRegimeTransitionResponse;
import br.dev.xb.isperp.mapper.FiscalRegimeTransitionMapper;
import br.dev.xb.isperp.service.FiscalRegimeTransitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@PreAuthorize("hasAnyRole('ADMIN', 'FINANCIAL')")
@SuppressWarnings("null")
public class FiscalRegimeTransitionController implements FiscalRegimeTransitionsApi {

    private final FiscalRegimeTransitionService regimeTransitionService;
    private final FiscalRegimeTransitionMapper regimeTransitionMapper;

    @Override
    public ResponseEntity<FiscalRegimeTransitionResponse> scheduleOrApplyTransition(
            FiscalRegimeTransitionRequest fiscalRegimeTransitionRequest) {
        var internalRequest = regimeTransitionMapper.toInternalRequest(fiscalRegimeTransitionRequest);
        var internalResponse = regimeTransitionService.scheduleOrApply(internalRequest);
        return ResponseEntity.ok(regimeTransitionMapper.toApiResponse(internalResponse));
    }

    @Override
    public ResponseEntity<List<FiscalRegimeTransitionResponse>> getTransitionHistory(UUID companyId) {
        var history = regimeTransitionService.getHistory(companyId);
        return ResponseEntity.ok(regimeTransitionMapper.toApiResponseList(history));
    }

    @Override
    public ResponseEntity<FiscalRegimeTransitionResponse> cancelTransition(UUID id) {
        var response = regimeTransitionService.cancelTransition(id);
        return ResponseEntity.ok(regimeTransitionMapper.toApiResponse(response));
    }

    @Override
    public ResponseEntity<Integer> triggerProcessPending() {
        int applied = regimeTransitionService.applyPendingTransitions();
        return ResponseEntity.ok(applied);
    }
}
