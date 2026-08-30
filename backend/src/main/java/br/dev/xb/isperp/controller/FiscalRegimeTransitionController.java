package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.dto.FiscalRegimeTransitionRequest;
import br.dev.xb.isperp.dto.FiscalRegimeTransitionResponse;
import br.dev.xb.isperp.service.FiscalRegimeTransitionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/fiscal/regimes/transitions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'FINANCIAL')")
public class FiscalRegimeTransitionController {

    private final FiscalRegimeTransitionService regimeTransitionService;

    @PostMapping
    public ResponseEntity<FiscalRegimeTransitionResponse> scheduleOrApply(
            @Valid @RequestBody FiscalRegimeTransitionRequest request) {
        FiscalRegimeTransitionResponse response = regimeTransitionService.scheduleOrApply(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<FiscalRegimeTransitionResponse>> getHistory(
            @RequestParam(required = false) UUID companyId) {
        List<FiscalRegimeTransitionResponse> history = regimeTransitionService.getHistory(companyId);
        return ResponseEntity.ok(history);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<FiscalRegimeTransitionResponse> cancelTransition(@PathVariable UUID id) {
        FiscalRegimeTransitionResponse response = regimeTransitionService.cancelTransition(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/process-pending")
    public ResponseEntity<Integer> triggerProcessPending() {
        int applied = regimeTransitionService.applyPendingTransitions();
        return ResponseEntity.ok(applied);
    }
}
