package com.xingubit.isperp.controller;

import com.xingubit.isperp.dto.SetupRequest;
import com.xingubit.isperp.dto.SetupResponse;
import com.xingubit.isperp.service.SetupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/setup")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class SetupController {
    
    private final SetupService setupService;
    
    @GetMapping("/status")
    public ResponseEntity<SetupResponse> getSetupStatus() {
        log.info("Verificando status do setup");
        SetupResponse response = setupService.getSetupStatus();
        return ResponseEntity.ok(response);
    }
    
    @PostMapping
    public ResponseEntity<SetupResponse> performSetup(@Valid @RequestBody SetupRequest request) {
        log.info("Iniciando processo de setup para empresa: {}", request.getCompanyName());
        
        try {
            SetupResponse response = setupService.performSetup(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Erro durante o setup: {}", e.getMessage());
            SetupResponse errorResponse = SetupResponse.builder()
                    .isSetupCompleted(false)
                    .setupStep(0)
                    .message("Erro durante o setup: " + e.getMessage())
                    .build();
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}